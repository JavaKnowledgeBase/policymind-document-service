package com.policymind.document.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.policymind.document.entity.DocumentChunk;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);

    long countByDocumentId(Long documentId);

    @Query(
            value = """
                    SELECT *
                    FROM document_chunk
                    WHERE document_id = :documentId
                      AND embedding_vector IS NOT NULL
                    ORDER BY embedding_vector <=> CAST(:queryVector AS vector)
                    LIMIT :limit
                    """,
            nativeQuery = true
    )
    List<DocumentChunk> findTopSimilarChunks(@Param("documentId") Long documentId,
                                             @Param("queryVector") String queryVector,
                                             @Param("limit") int limit);

    @Modifying
    @Transactional
    @Query(
            value = """
                    UPDATE document_chunk
                    SET embedding_vector = CAST(:vectorLiteral AS vector)
                    WHERE id = :chunkId
                    """,
            nativeQuery = true
    )
    void updateEmbeddingVector(@Param("chunkId") Long chunkId, @Param("vectorLiteral") String vectorLiteral);
}
