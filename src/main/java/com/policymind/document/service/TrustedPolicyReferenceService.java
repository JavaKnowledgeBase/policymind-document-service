package com.policymind.document.service;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrustedPolicyReferenceService {

    public List<ReferenceClause> getHrInternalPolicyReferences() {
        return List.of(
                new ReferenceClause(
                        "Purpose",
                        "purpose",
                        "HR Policy",
                        "This policy establishes consistent expectations for employees and managers and defines how the organization will administer the covered workplace practice.",
                        "PolicyMind HR Starter Library",
                        "mandatory_language,employment_compliance"
                ),
                new ReferenceClause(
                        "Scope",
                        "scope",
                        "HR Policy",
                        "This policy applies to all employees, managers, contractors, and other covered personnel unless a written exception is approved by Human Resources.",
                        "PolicyMind HR Starter Library",
                        "mandatory_language"
                ),
                new ReferenceClause(
                        "Eligibility And Responsibilities",
                        "responsibility",
                        "HR Policy",
                        "Employees must comply with the requirements in this policy, and managers must consistently review, approve, and document exceptions when they are permitted.",
                        "PolicyMind HR Starter Library",
                        "mandatory_language,approval_dependency"
                ),
                new ReferenceClause(
                        "Approval Workflow",
                        "approval",
                        "HR Policy",
                        "Requests under this policy must be submitted through the designated approval process and must receive manager or HR approval before taking effect.",
                        "PolicyMind HR Starter Library",
                        "mandatory_language,approval_dependency"
                ),
                new ReferenceClause(
                        "Enforcement",
                        "enforcement",
                        "HR Policy",
                        "Violations of this policy may result in corrective action, up to and including disciplinary action, consistent with applicable law and company procedure.",
                        "PolicyMind HR Starter Library",
                        "mandatory_language,enforcement_risk"
                )
        );
    }

    public record ReferenceClause(String sectionTitle,
                                  String clauseType,
                                  String policyType,
                                  String content,
                                  String sourceName,
                                  String riskTags) {
    }
}
