package com.campus.freshman.rag;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController @RequestMapping("/api/workspace")
public class WorkspaceController {
    private final KnowledgeWorkspace workspace; private final WorkspaceSearch search; private final ModelGateway gateway; private final AgentWorkspace agentService;
    private final ModelConnectionRepository models; private final CampusAgentRepository agents; private final RagRunRepository runs;
    public WorkspaceController(KnowledgeWorkspace workspace,WorkspaceSearch search,ModelGateway gateway,AgentWorkspace agentService,ModelConnectionRepository models,CampusAgentRepository agents,RagRunRepository runs) {
        this.workspace=workspace;this.search=search;this.gateway=gateway;this.agentService=agentService;this.models=models;this.agents=agents;this.runs=runs;
    }
    @GetMapping("/bases") public List<RagBase> bases() {return workspace.bases().findAll();}
    @PostMapping("/bases") public RagBase createBase(@RequestBody KnowledgeWorkspace.BaseInput body) {return workspace.saveBase(null,body);}
    @PutMapping("/bases/{id}") public RagBase updateBase(@PathVariable Long id,@RequestBody KnowledgeWorkspace.BaseInput body) {return workspace.saveBase(id,body);}
    @GetMapping("/bases/{id}/documents") public List<RagDocument> documents(@PathVariable Long id) {workspace.base(id); return workspace.documents().findByBaseIdOrderByUpdatedAtDesc(id);}
    @PostMapping("/bases/{id}/text") public RagDocument text(@PathVariable Long id,@RequestBody KnowledgeWorkspace.TextInput body) {return workspace.text(id,body);}
    @PostMapping("/bases/{id}/upload") public RagDocument upload(@PathVariable Long id,@RequestPart("file") MultipartFile file,@RequestParam(required=false) String title,@RequestParam(required=false) String source,@RequestParam(required=false) Long previousId) {return workspace.upload(id,file,title,source,previousId);}
    @PostMapping("/bases/{id}/migrate-legacy") public Map<String,Integer> migrate(@PathVariable Long id) {return Map.of("imported",workspace.migrate(id));}
    @PostMapping("/documents/{id}/action") public void action(@PathVariable Long id,@RequestBody KnowledgeWorkspace.DocumentAction body) {workspace.documentAction(id,body);}
    @GetMapping("/documents/{id}/chunks") public List<RagChunk> chunks(@PathVariable Long id) {workspace.document(id); return workspace.chunks().findByDocumentIdOrderByPositionAsc(id);}
    @PostMapping("/documents/{id}/preview") public List<Chunker.Piece> preview(@PathVariable Long id,@RequestBody KnowledgeWorkspace.SplitInput body) {return workspace.preview(id,body);}
    @PostMapping("/documents/{id}/split") public List<RagChunk> split(@PathVariable Long id,@RequestBody KnowledgeWorkspace.SplitInput body) {return workspace.split(id,body);}
    @PutMapping("/chunks/{id}") public RagChunk editChunk(@PathVariable Long id,@RequestBody KnowledgeWorkspace.ChunkInput body) {return workspace.editChunk(id,body);}
    @GetMapping("/bases/{id}/faqs") public List<RagFaq> faqs(@PathVariable Long id) {workspace.base(id); return workspace.faqs().findByBaseIdOrderByUpdatedAtDesc(id);}
    @PostMapping("/bases/{id}/faqs") public RagFaq createFaq(@PathVariable Long id,@RequestBody KnowledgeWorkspace.FaqInput body) {return workspace.saveFaq(id,null,body);}
    @PutMapping("/bases/{id}/faqs/{faqId}") public RagFaq updateFaq(@PathVariable Long id,@PathVariable Long faqId,@RequestBody KnowledgeWorkspace.FaqInput body) {return workspace.saveFaq(id,faqId,body);}
    @PostMapping("/bases/{id}/faqs/import-preview") public KnowledgeWorkspace.ImportPreview previewImport(@PathVariable Long id,@RequestPart("file") MultipartFile file) {return workspace.importPreview(id,file);}
    @PostMapping("/bases/{id}/faqs/import") public Map<String,Integer> commitImport(@PathVariable Long id,@RequestBody List<KnowledgeWorkspace.FaqInput> body) {return Map.of("imported",workspace.importFaqs(id,body));}
    @GetMapping("/bases/{id}/faqs/export") public ResponseEntity<byte[]> export(@PathVariable Long id) {
        workspace.base(id);StringBuilder csv=new StringBuilder("\uFEFF标准问题,相似问法,标准答案,来源\r\n");
        for(var faq:workspace.faqs().findByBaseIdOrderByUpdatedAtDesc(id)) csv.append(cell(faq.question)).append(',').append(cell(faq.aliases)).append(',').append(cell(faq.answer)).append(',').append(cell(faq.source)).append("\r\n");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=faq-export.csv").contentType(new MediaType("text","csv",StandardCharsets.UTF_8)).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }
    private String cell(String value) {String text=Objects.toString(value,"");if(text.matches("^[=+@\\-].*"))text="'"+text;return "\""+text.replace("\"","\"\"")+"\"";}
    @PostMapping("/bases/{id}/search") public WorkspaceSearch.SearchResult search(@PathVariable Long id,@RequestBody WorkspaceSearch.SearchInput body) {return search.search(id,body);}
    @GetMapping("/models") public List<ModelGateway.ConnectionView> models() {return models.findAll().stream().map(gateway::view).toList();}
    @PostMapping("/models") public ModelGateway.ConnectionView createModel(@RequestBody ModelGateway.ConnectionInput body) {return gateway.save(null,body);}
    @PutMapping("/models/{id}") public ModelGateway.ConnectionView updateModel(@PathVariable Long id,@RequestBody ModelGateway.ConnectionInput body) {return gateway.save(id,body);}
    @PostMapping("/models/{id}/test") public ModelGateway.ConnectionView testModel(@PathVariable Long id) {return gateway.test(id);}
    @GetMapping("/agents") public List<CampusAgent> agents() {return agents.findAll();}
    @PostMapping("/agents") public CampusAgent createAgent(@RequestBody AgentWorkspace.AgentInput body) {return agentService.save(null,body);}
    @PutMapping("/agents/{id}") public CampusAgent updateAgent(@PathVariable Long id,@RequestBody AgentWorkspace.AgentInput body) {return agentService.save(id,body);}
    @GetMapping("/agents/{id}/releases") public List<AgentRevision> releases(@PathVariable Long id) {return agentService.history(id);}
    @PostMapping("/agents/{id}/publish") public AgentRevision publish(@PathVariable Long id,@RequestBody Map<String,String> body) {return agentService.publish(id,body.get("note"));}
    @PostMapping("/agents/{id}/rollback/{releaseId}") public CampusAgent rollback(@PathVariable Long id,@PathVariable Long releaseId) {return agentService.rollback(id,releaseId);}
    @PostMapping("/agents/{id}/chat") public AgentWorkspace.Answer chat(@PathVariable Long id,@RequestBody AgentWorkspace.ChatInput body) {return agentService.chat(id,body);}
    @GetMapping("/runs") public List<RagRun> runs() {return runs.findTop50ByOrderByCreatedAtDesc();}
}
