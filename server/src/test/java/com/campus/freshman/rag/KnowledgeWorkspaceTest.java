package com.campus.freshman.rag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @Transactional
class KnowledgeWorkspaceTest {
    @Autowired KnowledgeWorkspace workspace; @Autowired WorkspaceSearch search; @Autowired AgentWorkspace agents; @Autowired ModelGateway gateway; @Autowired RagRunRepository runs;
    private RagBase base() {return workspace.saveBase(null,new KnowledgeWorkspace.BaseInput("测试知识库","仅H2事务测试",null,null,800,100,"GENERAL"));}
    private KnowledgeWorkspace.FaqInput faq(String question,String aliases,String answer,String condition,String status,LocalDate expires) {
        return new KnowledgeWorkspace.FaqInput(question,aliases,answer,"校园卡","信息中心测试资料",null,null,condition,"DIRECT",status,null,expires);
    }
    @Test void documentOnlyBecomesSearchableAfterPublishAndStopsImmediately() {
        var base=base();var doc=workspace.text(base.getId(),new KnowledgeWorkspace.TextInput("校园卡办理指南","校园卡领取地点是综合服务中心一层，请携带身份证。","信息中心",null,null));
        var input=new WorkspaceSearch.SearchInput("校园卡",null,null,5);assertThat(search.search(base.getId(),input).hits()).isEmpty();
        var chunks=workspace.split(doc.getId(),new KnowledgeWorkspace.SplitInput(800,100));assertThat(chunks).hasSize(1);assertThat(search.search(base.getId(),input).hits()).isEmpty();
        workspace.documentAction(doc.getId(),new KnowledgeWorkspace.DocumentAction("PUBLISH"));
        var hits=search.search(base.getId(),input).hits();assertThat(hits).hasSize(1);assertThat(hits.getFirst().version()).isEqualTo(1);
        assertThatThrownBy(()->workspace.editChunk(chunks.getFirst().getId(),new KnowledgeWorkspace.ChunkInput("不能改已发布内容",true))).hasMessageContaining("先停用");
        workspace.documentAction(doc.getId(),new KnowledgeWorkspace.DocumentAction("DISABLE"));assertThat(search.search(base.getId(),input).hits()).isEmpty();
    }
    @Test void approvedFaqDirectAnswerDoesNotInvokeModelAndLogsDebugTraffic() {
        var base=base();workspace.saveFaq(base.getId(),null,faq("校园卡在哪里领取？","饭卡在哪领；一卡通怎么领取","请到综合服务中心领取。",null,"PUBLISHED",null));
        var agent=agents.save(null,new AgentWorkspace.AgentInput("测试助手",base.getId(),null,"你是校园助手，日期{{current_date}}。"));
        var answer=agents.chat(agent.getId(),new AgentWorkspace.ChatInput("饭卡在哪领？",null,null,null,false));
        assertThat(answer.mode()).isEqualTo("FAQ_DIRECT");assertThat(answer.answer()).contains("综合服务中心");assertThat(answer.inputTokens()).isNull();assertThat(runs.findById(answer.runId()).orElseThrow().traffic).isEqualTo("DEBUG");
    }
    @Test void conditionalFaqRequiresClarification() {
        var base=base();workspace.saveFaq(base.getId(),null,faq("怎么领取补助？",null,"到学生处办理","仅限已提交资格审核的学生","PUBLISHED",null));
        var agent=agents.save(null,new AgentWorkspace.AgentInput("条件助手",base.getId(),null,"按证据回答"));
        var answer=agents.chat(agent.getId(),new AgentWorkspace.ChatInput("怎么领取补助？",null,null,null,false));assertThat(answer.mode()).isEqualTo("CLARIFY");assertThat(answer.answer()).contains("资格审核");
    }
    @Test void rejectsOverlappingFaqAndDoesNotMixCampuses() {
        var base=base();workspace.saveFaq(base.getId(),null,faq("校园卡在哪里领取？","饭卡在哪领","到服务中心",null,"PUBLISHED",null));
        assertThatThrownBy(()->workspace.saveFaq(base.getId(),null,faq("领取校园卡？","饭卡在哪领","到另一栋楼",null,"PUBLISHED",null))).hasMessageContaining("重叠问法");
    }
    @Test void unpublishedAndExpiredItemsAreExcluded() {
        var base=base();workspace.saveFaq(base.getId(),null,faq("过期领取流程",null,"旧流程",null,"PUBLISHED",LocalDate.now().minusDays(1)));
        workspace.saveFaq(base.getId(),null,faq("待审核领取流程",null,"未审核",null,"DRAFT",null));assertThat(search.search(base.getId(),new WorkspaceSearch.SearchInput("领取流程",null,null,5)).hits()).isEmpty();
    }
    @Test void importPreviewDoesNotWriteAndReportsDuplicateRows() {
        var base=base();String csv="标准问题,相似问法,标准答案,来源\n校园卡在哪领,饭卡在哪领,到服务中心,信息中心\n校园卡在哪领,饭卡在哪领,到服务中心,信息中心\n";
        var preview=workspace.importPreview(base.getId(),new MockMultipartFile("file","faq.csv","text/csv",csv.getBytes(StandardCharsets.UTF_8)));
        assertThat(preview.rows()).hasSize(2);assertThat(preview.errors()).isNotEmpty();assertThat(workspace.faqs().findByBaseIdOrderByUpdatedAtDesc(base.getId())).isEmpty();
    }
    @Test void missingModelKeyStopsBeforeNetworkRequest() {
        var value=gateway.save(null,new ModelGateway.ConnectionInput("缺密钥模型","CHAT","https://example.invalid/v1","test","CAMPUS_TEST_KEY_THAT_IS_NOT_SET"));
        assertThat(value.keyConfigured()).isFalse();var tested=gateway.test(value.id());assertThat(tested.testStatus()).isEqualTo("FAILED");assertThat(tested.testMessage()).contains("未发送模型请求");
    }
    @Test void rollbackDoesNotOverwriteDraftAndEmptyEvidenceDoesNotInvokeModel() {
        var base=base();var agent=agents.save(null,new AgentWorkspace.AgentInput("版本助手",base.getId(),null,"第一版"));
        var release=agents.publish(agent.getId(),"首次发布");agents.save(agent.getId(),new AgentWorkspace.AgentInput("版本助手",base.getId(),null,"第二版草稿"));
        agents.rollback(agent.getId(),release.getId());assertThat(agents.agent(agent.getId()).systemPrompt).isEqualTo("第二版草稿");
        assertThat(agents.chat(agent.getId(),new AgentWorkspace.ChatInput("未录入的信息",null,null,null,true)).mode()).isEqualTo("NO_EVIDENCE");
    }
    @Test void rejectsUnknownPromptVariables() {var base=base();assertThatThrownBy(()->agents.save(null,new AgentWorkspace.AgentInput("错误变量",base.getId(),null,"{{secret}}"))).hasMessageContaining("不支持的变量");}
}
