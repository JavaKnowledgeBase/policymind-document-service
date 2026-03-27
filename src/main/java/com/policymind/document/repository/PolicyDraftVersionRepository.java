package com.policymind.document.repository;

import com.policymind.document.model.PolicyDraftVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyDraftVersionRepository extends JpaRepository<PolicyDraftVersion, Long> {
    List<PolicyDraftVersion> findByDraft_IdOrderByVersionNumberDesc(Long draftId);
}

