package com.campus.freshman.rag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RagRunRepository extends JpaRepository<RagRun, Long> { List<RagRun> findTop50ByOrderByCreatedAtDesc(); }
