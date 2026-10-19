package com.campus.freshman.rag;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface RagDocumentRepository extends JpaRepository<RagDocument, Long> {
    List<RagDocument> findByBaseIdOrderByUpdatedAtDesc(Long baseId);
    List<RagDocument> findTop5ByProcessingOrderByCreatedAtAsc(String processing);
    boolean existsByLegacyId(Long legacyId);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select d from RagDocument d where d.id = :id")
    Optional<RagDocument> lockById(Long id);
}
