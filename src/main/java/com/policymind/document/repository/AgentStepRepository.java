package com.policymind.document.repository;

import com.policymind.document.model.AgentStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentStepRepository extends JpaRepository<AgentStep, Long> {
    List<AgentStep> findByAgentRunIdOrderByCreatedAtAsc(Long agentRunId);
}
