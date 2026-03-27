package com.policymind.document.dto;

import java.util.List;

public class PolicyComposeRequest {

    private String mode;
    private String provider;
    private String policyType;
    private String title;
    private String jurisdiction;
    private String audience;
    private String tone;
    private String sourceText;
    private String goals;
    private String additionalInstructions;
    private List<String> mustIncludeClauses;
    private List<String> prohibitedClauses;

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

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
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

    public List<String> getMustIncludeClauses() {
        return mustIncludeClauses;
    }

    public void setMustIncludeClauses(List<String> mustIncludeClauses) {
        this.mustIncludeClauses = mustIncludeClauses;
    }

    public List<String> getProhibitedClauses() {
        return prohibitedClauses;
    }

    public void setProhibitedClauses(List<String> prohibitedClauses) {
        this.prohibitedClauses = prohibitedClauses;
    }
}
