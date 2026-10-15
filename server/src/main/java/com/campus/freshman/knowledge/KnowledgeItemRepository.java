package com.campus.freshman.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long> {
    List<KnowledgeItem> findAllByOrderByUpdatedAtDesc();
    List<KnowledgeItem> findAllByDemoDataFalseOrderByUpdatedAtDesc();
    long countByStatus(String status);
    long countByStatusAndDemoDataFalse(String status);
    long countByExpiresAtBefore(LocalDate date);
    long countByExpiresAtBeforeAndDemoDataFalse(LocalDate date);
}
