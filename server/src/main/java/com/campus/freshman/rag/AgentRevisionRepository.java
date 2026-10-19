package com.campus.freshman.rag;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AgentRevisionRepository extends JpaRepository<AgentRevision, Long> { List<AgentRevision> findByAgentIdOrderByCreatedAtDesc(Long id); }
