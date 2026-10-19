package com.campus.freshman.rag;

import org.springframework.stereotype.Service;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.util.QueryBuilder;
import java.util.*;

@Service
public class WorkspaceSearch {
    private final KnowledgeWorkspace workspace;
    public WorkspaceSearch(KnowledgeWorkspace workspace) {this.workspace=workspace;}
    public SearchResult search(Long baseId,SearchInput input) {
        RagBase base=workspace.base(baseId); String query=KnowledgeWorkspace.required(input.query(),"检索问题",1000);
        String campus=input.campus()==null || input.campus().isBlank()?base.campus:input.campus();
        String audience=input.audience()==null || input.audience().isBlank()?base.audience:input.audience();
        if(!inScope(base.campus,campus) || !inScope(base.audience,audience)) throw KnowledgeWorkspace.bad("查询范围与当前知识库不符");
        List<Evidence> corpus=new ArrayList<>(); List<RagFaq> exact=new ArrayList<>();
        for(var faq:workspace.faqs().findByBaseIdOrderByUpdatedAtDesc(baseId)) {
            if(!KnowledgeWorkspace.effective(faq.status,faq.effectiveAt,faq.expiresAt) || !inScope(faq.campus,campus) || !inScope(faq.audience,audience)) continue;
            if(KnowledgeWorkspace.questions(faq).contains(KnowledgeWorkspace.normalize(query))) exact.add(faq);
            corpus.add(new Evidence("FAQ:"+faq.getId(),"FAQ",faq.getId(),null,null,faq.question,faq.answer+(faq.conditions==null||faq.conditions.isBlank()?"":"\n适用条件："+faq.conditions),faq.source,null,null,0,
                    faq.question+"\n"+Objects.toString(faq.aliases,"")+"\n"+faq.answer));
        }
        Map<Long,RagDocument> published=new HashMap<>();
        for(var doc:workspace.documents().findByBaseIdOrderByUpdatedAtDesc(baseId)) if("READY".equals(doc.processing) && KnowledgeWorkspace.effective(doc.status,doc.effectiveAt,doc.expiresAt)) published.put(doc.getId(),doc);
        Map<Long,RagChunk> chunkMap=new HashMap<>(); for(var chunk:workspace.chunks().findByBaseId(baseId)) chunkMap.put(chunk.getId(),chunk);
        for(var chunk:chunkMap.values()) {
            var doc=published.get(chunk.documentId); if(doc==null || !chunk.enabled || !"CHILD".equals(chunk.kind)) continue;
            RagChunk returned=chunk.parentId==null?chunk:chunkMap.get(chunk.parentId); if(returned==null || !returned.enabled) continue;
            corpus.add(new Evidence("CHUNK:"+chunk.getId(),"CHUNK",chunk.getId(),doc.getId(),doc.versionNumber,doc.title,returned.content,doc.source,chunk.page,chunk.section,0,chunk.content));
        }
        List<Evidence> hits=new ArrayList<>();
        try(var directory=new ByteBuffersDirectory();var analyzer=new SmartChineseAnalyzer()) {
            try(var writer=new IndexWriter(directory,new IndexWriterConfig(analyzer))) {
                for(int i=0;i<corpus.size();i++) {
                    var entry=new Document(); entry.add(new StoredField("position",i)); entry.add(new TextField("body",corpus.get(i).searchText(),Field.Store.NO)); writer.addDocument(entry);
                }
            }
            if(!corpus.isEmpty()) try(var reader=DirectoryReader.open(directory)) {
                Query parsed=new QueryBuilder(analyzer).createBooleanQuery("body",query);
                if(parsed!=null) {
                    var searcher=new IndexSearcher(reader); var results=searcher.search(parsed,Math.max(1,Math.min(20,input.topK()==null?6:input.topK())));
                    Set<String> seen=new HashSet<>();
                    for(var hit:results.scoreDocs) {
                        Evidence raw=corpus.get(searcher.storedFields().document(hit.doc).getField("position").numericValue().intValue());
                        String identity=raw.documentId()==null?raw.id():raw.documentId()+":"+raw.content();
                        if(seen.add(identity)) hits.add(new Evidence(raw.id(),raw.kind(),raw.recordId(),raw.documentId(),raw.version(),raw.title(),raw.content(),raw.source(),raw.page(),raw.section(),hit.score,null));
                    }
                }
            }
        } catch(Exception exception) {throw KnowledgeWorkspace.bad("全文检索失败，请检查索引数据后重试");}
        RagFaq direct=exact.size()==1 && "DIRECT".equals(exact.getFirst().answerMode) && (exact.getFirst().conditions==null || exact.getFirst().conditions.isBlank()) && simple(query)?exact.getFirst():null;
        String clarification=exact.size()>1?"存在多个精确候选，请补充适用条件":exact.size()==1 && exact.getFirst().conditions!=null && !exact.getFirst().conditions.isBlank()?"请先确认适用条件："+exact.getFirst().conditions:null;
        return new SearchResult(query,"FULLTEXT_BM25",false,direct==null?null:direct.getId(),direct==null?null:direct.answer,
                clarification!=null?clarification:direct==null?"未达到精确直答条件，返回全文候选；未启用语义直答":"已命中审核问答，可直接回答",hits,clarification);
    }
    static boolean inScope(String rule,String actual) {return rule==null || rule.isBlank() || rule.equals(actual);}
    static boolean simple(String query) {return !query.matches("(?s).*(顺便|然后|再去|并且|同时|路线|导航|现在|今天|此刻|是否开放|有没有施工|帮我规划).*") && query.length()<160;}
    public record SearchInput(String query,String campus,String audience,Integer topK) {}
    public record Evidence(String id,String kind,Long recordId,Long documentId,Integer version,String title,String content,String source,Integer page,String section,double score,String searchText) {}
    public record SearchResult(String query,String mode,boolean vectorAvailable,Long directFaqId,String directAnswer,String message,List<Evidence> hits,String clarification) {}
}
