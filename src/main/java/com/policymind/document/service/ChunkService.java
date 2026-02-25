/**
 * Splits large document text into smaller chunks.
 *
 * LLMs have token limits and perform better when given
 * smaller, focused pieces of text.
 *
 * This method divides long text into manageable segments
 * of approximately fixed size.
 *
 * Later these chunks will:
 * - Be embedded into vector form
 * - Stored in database
 * - Used for similarity search
 *
 * @param text Full extracted document text.
 * @return List of smaller text chunks.
 */
/**
 * ==============================================================
 * PolicyMind AI - ChunkService
 * ==============================================================
 *
 * Responsible for splitting large documents into smaller
 * semantic segments suitable for embedding and retrieval.
 *
 * Why chunking is required:
 * - LLM token limits
 * - Cost control
 * - More accurate semantic retrieval
 *
 * Each chunk becomes:
 * - A database row
 * - An embedding vector
 * - A candidate for similarity ranking
 */
package com.policymind.document.service;

import org.springframework.stereotype.Service;

import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Service
public class ChunkService {
	
	private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
	
	
    public ChunkService(DocumentChunkRepository chunkRepository) {
		this.chunkRepository = chunkRepository;
		this.embeddingService = new EmbeddingService();
	}
    
    private static final Logger logger = LoggerFactory.getLogger(ChunkService.class);

	public List<String> chunkText(String text) {
    	
    	
        int chunkSize = 800;
        List<String> chunks = new ArrayList<>();
        for (int i = 0; i < text.length(); i += chunkSize) {
            chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
        }
        logger.info("Chunk processed successfully");
        return chunks;
    }
	
	/**
	 * Saves document chunks and their embeddings to the database.
	 *
	 * For each chunk:
	 * 1. Generate embedding using EmbeddingService.
	 * 2. Store chunk content and embedding JSON.
	 *
	 * Why this exists:
	 * Embeddings allow semantic similarity search during query time.
	 *
	 * @param documentId ID of the parent document.
	 * @param chunks List of chunked document text.
	 */
    
    public void saveChunks(Document savedDoc, List<String> chunks) {

        
        for (String chunkText : chunks) {
        	
        	if (chunkText.length() > 3000) {
        	    chunkText = chunkText.substring(0, 3000);
        	}

            String embeddingJson =
                    embeddingService.generateEmbedding(chunkText);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(savedDoc);
            chunk.setContent(chunkText);
            chunk.setEmbedding(embeddingJson);

            chunkRepository.save(chunk);
        }
    }
}
