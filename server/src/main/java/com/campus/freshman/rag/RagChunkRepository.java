package com.campus.freshman.rag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {
    List<RagChunk> findByDocumentIdOrderByPositionAsc(Long documentId);
    List<RagChunk> findByBaseId(Long baseId);
    void deleteByDocumentId(Long documentId);
}
