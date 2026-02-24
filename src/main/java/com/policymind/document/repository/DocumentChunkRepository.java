package com.policymind.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.policymind.document.entity.DocumentChunk;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

	List<DocumentChunk> findByDocumentId(Long documentId);

}
