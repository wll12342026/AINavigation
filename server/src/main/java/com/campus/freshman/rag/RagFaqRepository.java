package com.campus.freshman.rag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RagFaqRepository extends JpaRepository<RagFaq, Long> { List<RagFaq> findByBaseIdOrderByUpdatedAtDesc(Long baseId); }
