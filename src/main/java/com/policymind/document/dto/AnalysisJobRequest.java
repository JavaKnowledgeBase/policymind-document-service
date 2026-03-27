package com.policymind.document.dto;

public class AnalysisJobRequest {

    private String question;
    private String embeddingProvider;
    private String answerProvider;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getEmbeddingProvider() {
        return embeddingProvider;
    }

    public void setEmbeddingProvider(String embeddingProvider) {
        this.embeddingProvider = embeddingProvider;
    }

    public String getAnswerProvider() {
        return answerProvider;
    }

    public void setAnswerProvider(String answerProvider) {
        this.answerProvider = answerProvider;
    }
}
