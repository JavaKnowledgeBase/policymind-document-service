package com.policymind.document.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_draft")
public class PolicyDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String policyType;
    private String mode;
    private String provider;
    private String jurisdiction;
    private String audience;
    private String tone;
    private Integer currentVersionNumber;
    private Integer latestQualityScore;
    private String latestConfidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastGeneratedAt;

    @Column(columnDefinition = "TEXT")
    private String goals;

    @Column(columnDefinition = "TEXT")
    private String additionalInstructions;

    @Column(columnDefinition = "TEXT")
    private String sourceText;

    @Column(columnDefinition = "TEXT")
    private String workingDraft;

    @Column(columnDefinition = "TEXT")
    private String latestSummary;

    @Column(columnDefinition = "TEXT")
    private String latestRationale;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public Integer getCurrentVersionNumber() {
        return currentVersionNumber;
    }

    public void setCurrentVersionNumber(Integer currentVersionNumber) {
        this.currentVersionNumber = currentVersionNumber;
    }

    public Integer getLatestQualityScore() {
        return latestQualityScore;
    }

    public void setLatestQualityScore(Integer latestQualityScore) {
        this.latestQualityScore = latestQualityScore;
    }

    public String getLatestConfidence() {
        return latestConfidence;
    }

    public void setLatestConfidence(String latestConfidence) {
        this.latestConfidence = latestConfidence;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastGeneratedAt() {
        return lastGeneratedAt;
    }

    public void setLastGeneratedAt(LocalDateTime lastGeneratedAt) {
        this.lastGeneratedAt = lastGeneratedAt;
    }

    public String getGoals() {
        return goals;
    }

    public void setGoals(String goals) {
        this.goals = goals;
    }

    public String getAdditionalInstructions() {
        return additionalInstructions;
    }

    public void setAdditionalInstructions(String additionalInstructions) {
        this.additionalInstructions = additionalInstructions;
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

    public String getLatestSummary() {
        return latestSummary;
    }

    public void setLatestSummary(String latestSummary) {
        this.latestSummary = latestSummary;
    }

    public String getLatestRationale() {
        return latestRationale;
    }

    public void setLatestRationale(String latestRationale) {
        this.latestRationale = latestRationale;
    }
}
