package com.campus.freshman.rag;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.util.*;

@Component @EnableScheduling
public class DocumentJobs {
    private final KnowledgeWorkspace workspace; private final TransactionTemplate transaction;
    public DocumentJobs(KnowledgeWorkspace workspace,PlatformTransactionManager manager) {this.workspace=workspace; transaction=new TransactionTemplate(manager);}
    @EventListener(ApplicationReadyEvent.class)
    public void recover() { transaction.executeWithoutResult(status -> {
        for(var doc:workspace.documents().findAll()) if("PARSING".equals(doc.processing)) {doc.processing="QUEUED"; doc.generation++; workspace.documents().save(doc);}
    }); }
    @Scheduled(fixedDelayString="${campus.document-poll-ms:1500}")
    public void tick() {
        var queued=workspace.documents().findTop5ByProcessingOrderByCreatedAtAsc("QUEUED");
        for(var item:queued) process(item.getId());
    }
    public void process(Long id) {
        RagDocument claimed=transaction.execute(status -> {
            var doc=workspace.documents().lockById(id).orElse(null);
            if(doc==null || !"QUEUED".equals(doc.processing) || "ARCHIVED".equals(doc.status)) return null;
            doc.processing="PARSING"; doc.attempts++; doc.error=null; return workspace.documents().saveAndFlush(doc);
        });
        if(claimed==null) return;
        try {
            var parsed=workspace.parser().parse(workspace.resolveFile(claimed.storageName),claimed.format);
            transaction.executeWithoutResult(status -> {
                var current=workspace.documents().lockById(id).orElse(null);
                if(current==null || current.generation!=claimed.generation || !"PARSING".equals(current.processing)) return;
                current.content=parsed.text(); current.warning=parsed.warning(); current.processing="AWAITING_CHUNKS"; workspace.documents().save(current);
            });
        } catch(Exception exception) {
            transaction.executeWithoutResult(status -> {
                var current=workspace.documents().lockById(id).orElse(null);
                if(current==null || current.generation!=claimed.generation || !"PARSING".equals(current.processing)) return;
                current.processing="FAILED"; current.error=exception instanceof IllegalArgumentException?exception.getMessage():"解析失败：文件损坏、类型不符或不支持，请检查原文件后重试";
                workspace.documents().save(current);
            });
        }
    }
}
