package com.campus.freshman.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {
    List<KnowledgeItem> findAllByOrderByUpdatedAtDesc();
    long countByStatus(String status);
    long countByExpiresAtBefore(LocalDate date);
}
