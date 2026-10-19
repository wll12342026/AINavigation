package com.campus.freshman.rag;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RagBaseRepository extends JpaRepository<RagBase, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select b from RagBase b where b.id = :id")
    java.util.Optional<RagBase> lockById(Long id);
}
