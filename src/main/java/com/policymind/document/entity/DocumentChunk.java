package com.policymind.document.entity;

import com.policymind.document.model.Document;

import jakarta.persistence.*;

@Entity
@Table(name = "document_chunk")
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String embedding;

    private Integer startLine;

    private Integer endLine;

    @Column(length = 255)
    private String chunkKind;

    @Column(length = 255)
    private String sectionTitle;

    @Column(length = 255)
    private String clauseType;

    @Column(length = 255)
    private String domain;

    @Column(length = 255)
    private String policyType;

    @Column(length = 255)
    private String jurisdiction;

    @Column(length = 255)
    private String sourceName;

    @Column(columnDefinition = "TEXT")
    private String riskTags;

    private Boolean referenceClause;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }
    public Integer getStartLine() { return startLine; }
    public void setStartLine(Integer startLine) { this.startLine = startLine; }
    public Integer getEndLine() { return endLine; }
    public void setEndLine(Integer endLine) { this.endLine = endLine; }
    public String getChunkKind() { return chunkKind; }
    public void setChunkKind(String chunkKind) { this.chunkKind = chunkKind; }
    public String getSectionTitle() { return sectionTitle; }
    public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }
    public String getClauseType() { return clauseType; }
    public void setClauseType(String clauseType) { this.clauseType = clauseType; }
    public String getDomain() { return domain; }
    public void setDomain(String domain) { this.domain = domain; }
    public String getPolicyType() { return policyType; }
    public void setPolicyType(String policyType) { this.policyType = policyType; }
    public String getJurisdiction() { return jurisdiction; }
    public void setJurisdiction(String jurisdiction) { this.jurisdiction = jurisdiction; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getRiskTags() { return riskTags; }
    public void setRiskTags(String riskTags) { this.riskTags = riskTags; }
    public Boolean getReferenceClause() { return referenceClause; }
    public void setReferenceClause(Boolean referenceClause) { this.referenceClause = referenceClause; }
}
