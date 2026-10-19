package com.campus.freshman.rag;

import com.campus.freshman.knowledge.KnowledgeItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

@Service
public class KnowledgeWorkspace {
    final RagBaseRepository bases; final RagDocumentRepository documents; final RagChunkRepository chunks;
    final RagFaqRepository faqs; final KnowledgeItemRepository legacy; final Chunker chunker; final DocumentParser parser;
    final Path storage;
    public KnowledgeWorkspace(RagBaseRepository bases, RagDocumentRepository documents, RagChunkRepository chunks,
                              RagFaqRepository faqs, KnowledgeItemRepository legacy, Chunker chunker, DocumentParser parser,
                              @Value("${campus.knowledge-dir:data/knowledge}") String directory) {
        this.bases=bases; this.documents=documents; this.chunks=chunks; this.faqs=faqs; this.legacy=legacy; this.chunker=chunker; this.parser=parser;
        storage=Path.of(directory).toAbsolutePath().normalize();
    }
    static ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    public RagBaseRepository bases() { return bases; }
    public RagDocumentRepository documents() { return documents; }
    public RagChunkRepository chunks() { return chunks; }
    public RagFaqRepository faqs() { return faqs; }
    public DocumentParser parser() { return parser; }
    static ResponseStatusException missing(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    static String required(String value, String label, int max) {
        if (value==null || value.isBlank() || value.length()>max) throw bad(label+"不能为空且不能超过"+max+"字符");
        return value.trim();
    }
    static void dates(LocalDate from, LocalDate to) { if (from!=null && to!=null && to.isBefore(from)) throw bad("有效期结束日期不能早于生效日期"); }
    static boolean effective(String status, LocalDate from, LocalDate to) {
        LocalDate now=LocalDate.now(java.time.ZoneId.of("Asia/Shanghai"));
        return "PUBLISHED".equals(status) && (from==null || !from.isAfter(now)) && (to==null || !to.isBefore(now));
    }
    public RagBase base(Long id) { return bases.findById(id).orElseThrow(() -> missing("知识库不存在")); }
    public RagDocument document(Long id) { return documents.findById(id).orElseThrow(() -> missing("文档不存在")); }
    @Transactional
    public RagBase saveBase(Long id, BaseInput input) {
        RagBase value=id==null?new RagBase():base(id);
        value.name=required(input.name(),"知识库名称",100); value.description=input.description(); value.campus=input.campus(); value.audience=input.audience();
        int size=input.chunkSize()==null?800:input.chunkSize(), overlap=input.overlap()==null?100:input.overlap();
        chunker.split("参数校验",size,overlap,false);
        String mode=input.chunkMode()==null?"GENERAL":input.chunkMode();
        if (!Set.of("GENERAL","PARENT_CHILD").contains(mode)) throw bad("切片模式不合法");
        value.chunkSize=size; value.overlap=overlap; value.chunkMode=mode; return bases.save(value);
    }
    @Transactional
    public RagDocument text(Long baseId, TextInput input) {
        base(baseId); dates(input.effectiveAt(),input.expiresAt()); RagDocument value=new RagDocument();
        value.baseId=baseId; value.title=required(input.title(),"标题",200); value.content=required(input.content(),"正文",DocumentParser.MAX_TEXT);
        value.source=input.source(); value.effectiveAt=input.effectiveAt(); value.expiresAt=input.expiresAt(); value.processing="AWAITING_CHUNKS";
        return documents.save(value);
    }
    @Transactional
    public RagDocument upload(Long baseId, MultipartFile file, String title, String source, Long previousId) {
        base(baseId);
        if (file.isEmpty() || file.getSize()>15L*1024*1024) throw bad("文件为空或超过15MB");
        String name=Objects.toString(file.getOriginalFilename(),"file");
        String ext=name.contains(".")?name.substring(name.lastIndexOf('.')+1).toUpperCase(Locale.ROOT):"";
        if (!Set.of("PDF","DOCX","MD","TXT").contains(ext)) throw bad("仅支持 PDF、DOCX、Markdown、TXT");
        RagDocument value=new RagDocument(); value.baseId=baseId; value.title=title==null || title.isBlank()?name:required(title,"标题",200);
        value.source=source; value.originalName=name; value.format=ext; value.fileSize=file.getSize();
        try {
            byte[] bytes=file.getBytes(); value.fileHash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
            if (previousId!=null) {
                RagDocument prior=document(previousId);
                if (!prior.baseId.equals(baseId)) throw bad("文档不属于当前知识库");
                value.rootId=prior.rootId==null?prior.getId():prior.rootId;
                if (documents.findByBaseIdOrderByUpdatedAtDesc(baseId).stream().anyMatch(d -> Objects.equals(d.rootId,value.rootId) && !"ARCHIVED".equals(d.status) && !"PUBLISHED".equals(d.status))) throw bad("此文档已有未发布版本，请先处理或归档");
                value.versionNumber=documents.findByBaseIdOrderByUpdatedAtDesc(baseId).stream()
                        .filter(d -> Objects.equals(d.getId(),value.rootId) || Objects.equals(d.rootId,value.rootId)).mapToInt(d -> d.versionNumber).max().orElse(1)+1;
                value.effectiveAt=prior.effectiveAt; value.expiresAt=prior.expiresAt;
            } else if (documents.findByBaseIdOrderByUpdatedAtDesc(baseId).stream().anyMatch(d -> value.fileHash.equals(d.fileHash) && !"ARCHIVED".equals(d.status))) throw bad("此文件已存在，请更新文档版本或使用现有记录");
            Files.createDirectories(storage); value.storageName=UUID.randomUUID()+"."+ext.toLowerCase(Locale.ROOT);
            Files.write(resolveFile(value.storageName),bytes,StandardOpenOption.CREATE_NEW);
            return documents.save(value);
        } catch (ResponseStatusException exception) { throw exception; }
        catch (Exception exception) { throw bad("文件保存失败，请检查存储目录"); }
    }
    Path resolveFile(String name) {
        Path value=storage.resolve(name).normalize();
        if (!value.startsWith(storage) || !value.getParent().equals(storage)) throw bad("文件路径不合法");
        return value;
    }
    @Transactional
    public void documentAction(Long id, DocumentAction input) {
        RagDocument snapshot=document(id);
        bases.lockById(snapshot.baseId).orElseThrow(() -> missing("知识库不存在"));
        RagDocument value=documents.lockById(id).orElseThrow(() -> missing("文档不存在"));
        switch (required(input.action(),"操作",30)) {
            case "PUBLISH" -> {
                if (!"READY".equals(value.processing)) throw bad("请先生成切片并确认全文索引就绪");
                if (chunks.findByDocumentIdOrderByPositionAsc(id).stream().noneMatch(c -> c.enabled && "CHILD".equals(c.kind))) throw bad("没有可用切片");
                dates(value.effectiveAt,value.expiresAt);
                if (value.expiresAt!=null && value.expiresAt.isBefore(LocalDate.now())) throw bad("文档已过期，不能发布");
                Long root=value.rootId==null?id:value.rootId;
                for (RagDocument other:documents.findByBaseIdOrderByUpdatedAtDesc(value.baseId)) {
                    if (!other.getId().equals(id) && (other.getId().equals(root)||Objects.equals(other.rootId,root)) && "PUBLISHED".equals(other.status)) { other.status="ARCHIVED"; documents.save(other); }
                }
                value.status="PUBLISHED";
            }
            case "DISABLE" -> value.status="DISABLED";
            case "ARCHIVE" -> { value.status="ARCHIVED"; value.generation++; if (Set.of("QUEUED","PARSING").contains(value.processing)) value.processing="CANCELLED"; }
            case "CANCEL" -> { value.processing="CANCELLED"; value.generation++; }
            case "RETRY" -> { if (!Set.of("FAILED","CANCELLED").contains(value.processing)) throw bad("仅失败或取消任务可重试"); value.processing=value.storageName==null?"AWAITING_CHUNKS":"QUEUED"; value.generation++; value.error=null; }
            default -> throw bad("不支持的文档操作");
        }
        documents.save(value);
    }
    public List<Chunker.Piece> preview(Long id, SplitInput input) {
        RagDocument value=document(id);
        if (value.content==null || value.content.isBlank()) throw bad("文档尚未解析完成");
        try { return chunker.split(value.content,input.size(),input.overlap(),"PDF".equals(value.format)); }
        catch (IllegalArgumentException exception) { throw bad(exception.getMessage()); }
    }
    @Transactional
    public List<RagChunk> split(Long id, SplitInput input) {
        RagDocument doc=documents.lockById(id).orElseThrow(() -> missing("文档不存在"));
        if ("PUBLISHED".equals(doc.status)) throw bad("已发布文档不可原地重新切片，请先停用或创建新版本");
        if (!Set.of("AWAITING_CHUNKS","READY").contains(doc.processing)) throw bad("文档尚未准备好切片");
        List<Chunker.Piece> pieces=preview(id,input);
        if (pieces.isEmpty()) throw bad("没有生成有效切片");
        chunks.deleteByDocumentId(id); chunks.flush(); int position=0;
        RagBase config=base(doc.baseId);
        if ("PARENT_CHILD".equals(config.chunkMode)) {
            List<Chunker.Piece> parents=chunker.split(doc.content,Math.min(4000,input.size()*3),0,"PDF".equals(doc.format));
            for (var piece:parents) {
                RagChunk parent=piece(doc,piece,position++); parent.kind="PARENT"; parent=chunks.save(parent);
                for (var childPiece:chunker.split(piece.content(),input.size(),input.overlap(),false)) {
                    RagChunk child=piece(doc,new Chunker.Piece(childPiece.content(),piece.page(),piece.section()),position++); child.parentId=parent.getId(); chunks.save(child);
                }
            }
        } else for (var piece:pieces) chunks.save(piece(doc,piece,position++));
        doc.processing="READY"; doc.generation++; documents.save(doc);
        return chunks.findByDocumentIdOrderByPositionAsc(id);
    }
    private RagChunk piece(RagDocument doc, Chunker.Piece piece, int position) {
        RagChunk value=new RagChunk(); value.baseId=doc.baseId; value.documentId=doc.getId(); value.position=position;
        value.content=piece.content(); value.page=piece.page(); value.section=piece.section(); return value;
    }
    @Transactional
    public RagChunk editChunk(Long id, ChunkInput input) {
        RagChunk value=chunks.findById(id).orElseThrow(() -> missing("切片不存在"));
        RagDocument doc=documents.lockById(value.documentId).orElseThrow();
        if ("PUBLISHED".equals(doc.status)) throw bad("请先停用文档再编辑切片");
        if (value.parentId!=null || "PARENT".equals(value.kind)) throw bad("父子模式请通过修改正文重新切片，避免父子内容不一致");
        value.content=required(input.content(),"切片正文",4000); value.enabled=input.enabled(); return chunks.save(value);
    }
    static String normalize(String text) { return Objects.toString(text,"").toLowerCase(Locale.ROOT).replaceAll("[\\p{P}\\p{Z}\\s]+",""); }
    static Set<String> questions(RagFaq value) {
        Set<String> result=new LinkedHashSet<>(); result.add(normalize(value.question));
        for(String alias:Objects.toString(value.aliases,"").split("[\\n；;]+")) if (!alias.isBlank()) result.add(normalize(alias));
        return result;
    }
    static boolean overlaps(String a,String b) { return a==null || a.isBlank() || b==null || b.isBlank() || a.equals(b); }
    @Transactional
    public RagFaq saveFaq(Long baseId,Long id,FaqInput input) {
        bases.lockById(baseId).orElseThrow(() -> missing("知识库不存在")); dates(input.effectiveAt(),input.expiresAt());
        RagFaq value=id==null?new RagFaq():faqs.findById(id).orElseThrow(() -> missing("问答不存在"));
        if (id!=null && !baseId.equals(value.baseId)) throw missing("问答不属于当前知识库");
        value.baseId=baseId; value.question=required(input.question(),"标准问题",255); value.answer=required(input.answer(),"标准答案",12000);
        value.aliases=input.aliases(); value.category=input.category(); value.source=input.source(); value.campus=input.campus(); value.audience=input.audience();
        value.conditions=input.conditions(); value.effectiveAt=input.effectiveAt(); value.expiresAt=input.expiresAt();
        value.answerMode=input.answerMode()==null?"DIRECT":input.answerMode(); value.status=input.status()==null?"DRAFT":input.status();
        if (!Set.of("DIRECT","RAG","TOOL").contains(value.answerMode) || !Set.of("DRAFT","PENDING","PUBLISHED","DISABLED").contains(value.status)) throw bad("问答模式或状态不合法");
        if ("PUBLISHED".equals(value.status)) {
            required(value.source,"发布来源",255);
            for (RagFaq other:faqs.findByBaseIdOrderByUpdatedAtDesc(baseId)) {
                boolean datesOverlap=(value.expiresAt==null || other.effectiveAt==null || !value.expiresAt.isBefore(other.effectiveAt)) && (other.expiresAt==null || value.effectiveAt==null || !other.expiresAt.isBefore(value.effectiveAt));
                if (!Objects.equals(other.getId(),id) && "PUBLISHED".equals(other.status) && datesOverlap && overlaps(value.campus,other.campus) && overlaps(value.audience,other.audience)
                        && !Collections.disjoint(questions(value),questions(other))) throw bad("与已发布问答“"+other.question+"”存在重叠问法，请先解决冲突");
            }
        }
        return faqs.save(value);
    }
    public ImportPreview importPreview(Long baseId, MultipartFile file) {
        base(baseId); if (file.isEmpty() || file.getSize()>5L*1024*1024) throw bad("问答表格为空或超过5MB");
        String name=Objects.toString(file.getOriginalFilename(),"").toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv") && !name.endsWith(".xlsx")) throw bad("问答表格仅支持 CSV 或 XLSX");
        Path temporary=null;
        try {
            Files.createDirectories(storage); temporary=Files.createTempFile(storage,"faq-import-",name.endsWith(".csv")?".csv":".xlsx"); file.transferTo(temporary);
            List<List<String>> table=parser.readFaqTable(temporary,name.endsWith(".csv")?"CSV":"XLSX");
            if (table.size()<2) throw bad("表格需包含标题行和数据；列名：标准问题、相似问法、标准答案、来源");
            List<String> headers=table.getFirst(); int q=column(headers,"标准问题","question"),a=column(headers,"标准答案","answer"),aliases=column(headers,"相似问法","aliases"),source=column(headers,"来源","source");
            if(q<0 || a<0) throw bad("缺少标准问题或标准答案列");
            List<FaqInput> rows=new ArrayList<>(); List<String> errors=new ArrayList<>(); Set<String> seen=new HashSet<>();
            for(int i=1;i<table.size();i++) {
                var row=table.get(i); if(row.stream().allMatch(String::isBlank)) continue;
                String question=cell(row,q),answer=cell(row,a),alias=cell(row,aliases);
                if(question.isBlank() || answer.isBlank()) { errors.add("第"+(i+1)+"行问题或答案为空"); continue; }
                if(question.length()>255 || answer.length()>12000) { errors.add("第"+(i+1)+"行内容过长"); continue; }
                RagFaq candidate=new RagFaq(); candidate.question=question; candidate.aliases=alias;
                Set<String> phrases=questions(candidate);
                if(phrases.stream().anyMatch(seen::contains)) errors.add("第"+(i+1)+"行问法与前面的记录重复");
                seen.addAll(phrases);
                if(faqs.findByBaseIdOrderByUpdatedAtDesc(baseId).stream().anyMatch(existing -> !Collections.disjoint(questions(existing),phrases))) errors.add("第"+(i+1)+"行问法与知识库现有问答重复");
                rows.add(new FaqInput(question,alias,answer,"导入问答",cell(row,source),null,null,null,"DIRECT","DRAFT",null,null));
            }
            return new ImportPreview(rows,errors);
        } catch(ResponseStatusException exception) {throw exception;}
        catch(Exception exception) {throw bad("表格读取失败，请检查格式、编码和内容");}
        finally {if(temporary!=null) try{Files.deleteIfExists(temporary);}catch(Exception ignored){}}
    }
    private int column(List<String> headers,String cn,String en) { for(int i=0;i<headers.size();i++) if(headers.get(i).equals(cn)||headers.get(i).equalsIgnoreCase(en)) return i; return -1; }
    private String cell(List<String> values,int index) {return index<0||index>=values.size()?"":values.get(index).trim();}
    @Transactional
    public int importFaqs(Long baseId,List<FaqInput> inputs) {
        if(inputs.size()>1000) throw bad("最多导入1000条问答"); Set<String> seen=new HashSet<>();
        for(FaqInput input:inputs) {
            RagFaq candidate=new RagFaq(); candidate.question=input.question(); candidate.aliases=input.aliases(); Set<String> phrases=questions(candidate);
            if(phrases.stream().anyMatch(seen::contains) || faqs.findByBaseIdOrderByUpdatedAtDesc(baseId).stream().anyMatch(existing -> !Collections.disjoint(questions(existing),phrases))) throw bad("存在重复问法，请修复后重新导入");
            seen.addAll(phrases);
            saveFaq(baseId,null,new FaqInput(input.question(),input.aliases(),input.answer(),input.category(),input.source(),input.campus(),input.audience(),input.conditions(),input.answerMode(),"DRAFT",input.effectiveAt(),input.expiresAt()));
        }
        return inputs.size();
    }
    @Transactional
    public int migrate(Long baseId) {
        base(baseId); int count=0;
        for(var item:legacy.findAllByDemoDataFalseOrderByUpdatedAtDesc()) if(!documents.existsByLegacyId(item.getId()) && item.getContent()!=null && !item.getContent().isBlank()) {
            RagDocument value=text(baseId,new TextInput(item.getTitle(),item.getContent(),item.getSource(),null,item.getExpiresAt())); value.legacyId=item.getId(); documents.save(value); count++;
        }
        return count;
    }
    public record BaseInput(String name,String description,String campus,String audience,Integer chunkSize,Integer overlap,String chunkMode) {}
    public record TextInput(String title,String content,String source,LocalDate effectiveAt,LocalDate expiresAt) {}
    public record SplitInput(int size,int overlap) {}
    public record ChunkInput(String content,boolean enabled) {}
    public record DocumentAction(String action) {}
    public record FaqInput(String question,String aliases,String answer,String category,String source,String campus,String audience,String conditions,String answerMode,String status,LocalDate effectiveAt,LocalDate expiresAt) {}
    public record ImportPreview(List<FaqInput> rows,List<String> errors) {}
}
