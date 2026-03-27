package com.policymind.document.repository;

import com.policymind.document.model.PolicyDraft;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyDraftRepository extends JpaRepository<PolicyDraft, Long> {
    List<PolicyDraft> findAllByOrderByUpdatedAtDesc();
}
