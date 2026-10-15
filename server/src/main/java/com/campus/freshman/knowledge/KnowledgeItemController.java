package com.campus.freshman.knowledge;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeItemController {
    private final KnowledgeItemRepository repository;
    public KnowledgeItemController(KnowledgeItemRepository repository) { this.repository = repository; }

    @GetMapping public List<KnowledgeItem> list() { return repository.findAllByOrderByUpdatedAtDesc(); }
    @GetMapping("/{id}") public KnowledgeItem detail(@PathVariable Long id) { return find(id); }
    @PostMapping public KnowledgeItem create(@Valid @RequestBody KnowledgeItem item) { item.setId(null); return repository.save(item); }
    @PutMapping("/{id}") public KnowledgeItem update(@PathVariable Long id, @Valid @RequestBody KnowledgeItem input) {
        KnowledgeItem item = find(id);
        item.setTitle(input.getTitle()); item.setCategory(input.getCategory()); item.setContent(input.getContent());
        item.setStatus(input.getStatus()); item.setSource(input.getSource()); item.setExpiresAt(input.getExpiresAt());
        return repository.save(item);
    }
    @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { repository.delete(find(id)); }
    private KnowledgeItem find(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("知识条目不存在")); }
}
