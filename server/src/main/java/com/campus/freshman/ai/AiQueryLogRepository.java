package com.campus.freshman.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiQueryLogRepository extends JpaRepository<AiQueryLog, Long> {
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countBySuccessfulTrueAndCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByIntentAndCreatedAtBetween(String intent, LocalDateTime start, LocalDateTime end);
    List<AiQueryLog> findAllByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime start, LocalDateTime end);
}
