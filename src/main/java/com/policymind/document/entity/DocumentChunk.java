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
	    private String embedding;  // store JSON vector temporarily

        private Integer startLine;

        private Integer endLine;


		public Document getDocument() {
			return document;
		}

		public void setDocument(Document document) {
			this.document = document;
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getEmbedding() {
			return embedding;
		}

		public void setEmbedding(String embedding) {
			this.embedding = embedding;
		}

        public Integer getStartLine() {
            return startLine;
        }

        public void setStartLine(Integer startLine) {
            this.startLine = startLine;
        }

        public Integer getEndLine() {
            return endLine;
        }

        public void setEndLine(Integer endLine) {
            this.endLine = endLine;
        }

}
