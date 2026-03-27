package com.policymind.document.repository;

import com.policymind.document.model.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentRunRepository extends JpaRepository<AgentRun, Long> {
    Optional<AgentRun> findByAnalysisJobId(Long analysisJobId);
}
