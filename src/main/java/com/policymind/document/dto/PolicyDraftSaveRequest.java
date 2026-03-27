package com.policymind.document.dto;

public class PolicyDraftSaveRequest {

    private Long draftId;
    private String mode;
    private String provider;
    private String policyType;
    private String title;
    private String jurisdiction;
    private String audience;
    private String tone;
    private String goals;
    private String additionalInstructions;
    private String sourceText;
    private String workingDraft;
    private String summary;
    private String rationale;
    private String confidence;
    private Integer qualityScore;
    private Object keyChanges;
    private Object implementationChecklist;
    private Object riskFlags;
    private Object composeResult;

    public Long getDraftId() {
        return draftId;
    }

    public void setDraftId(Long draftId) {
        this.draftId = draftId;
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

    public String getPolicyType() {
        return policyType;
    }

    public void setPolicyType(String policyType) {
        this.policyType = policyType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public String getConfidence() {
        return confidence;
    }

    public void setConfidence(String confidence) {
        this.confidence = confidence;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Object getKeyChanges() {
        return keyChanges;
    }

    public void setKeyChanges(Object keyChanges) {
        this.keyChanges = keyChanges;
    }

    public Object getImplementationChecklist() {
        return implementationChecklist;
    }

    public void setImplementationChecklist(Object implementationChecklist) {
        this.implementationChecklist = implementationChecklist;
    }

    public Object getRiskFlags() {
        return riskFlags;
    }

    public void setRiskFlags(Object riskFlags) {
        this.riskFlags = riskFlags;
    }

    public Object getComposeResult() {
        return composeResult;
    }

    public void setComposeResult(Object composeResult) {
        this.composeResult = composeResult;
    }
}
