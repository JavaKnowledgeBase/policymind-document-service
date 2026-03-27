package com.policymind.document.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_draft_version")
public class PolicyDraftVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "draft_id")
    private PolicyDraft draft;

    private Integer versionNumber;
    private Integer qualityScore;
    private String confidence;
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String rationale;

    @Column(columnDefinition = "TEXT")
    private String sourceText;

    @Column(columnDefinition = "TEXT")
    private String workingDraft;

    @Column(columnDefinition = "TEXT")
    private String keyChangesJson;

    @Column(columnDefinition = "TEXT")
    private String implementationChecklistJson;

    @Column(columnDefinition = "TEXT")
    private String riskFlagsJson;

    @Column(columnDefinition = "TEXT")
    private String composeResultJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PolicyDraft getDraft() {
        return draft;
    }

    public void setDraft(PolicyDraft draft) {
        this.draft = draft;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getWorkingDraft() {
        return workingDraft;
    }

    public void setWorkingDraft(String workingDraft) {
        this.workingDraft = workingDraft;
    }

    public String getKeyChangesJson() {
        return keyChangesJson;
    }

    public void setKeyChangesJson(String keyChangesJson) {
        this.keyChangesJson = keyChangesJson;
    }

    public String getImplementationChecklistJson() {
        return implementationChecklistJson;
    }

    public void setImplementationChecklistJson(String implementationChecklistJson) {
        this.implementationChecklistJson = implementationChecklistJson;
    }

    public String getRiskFlagsJson() {
        return riskFlagsJson;
    }

    public void setRiskFlagsJson(String riskFlagsJson) {
        this.riskFlagsJson = riskFlagsJson;
    }

    public String getComposeResultJson() {
        return composeResultJson;
    }

    public void setComposeResultJson(String composeResultJson) {
        this.composeResultJson = composeResultJson;
    }
}
