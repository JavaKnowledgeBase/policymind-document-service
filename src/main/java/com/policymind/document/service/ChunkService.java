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

    private static final int CHUNK_SIZE = 800;
    private final DocumentChunkRepository chunkRepository;
    private static final Logger logger = LoggerFactory.getLogger(ChunkService.class);

    public ChunkService(DocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    // ================================
    // TEXT CHUNKING
    // ================================
    public List<String> chunkText(String text) {

        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += CHUNK_SIZE) {
            chunks.add(text.substring(i, Math.min(text.length(), i + CHUNK_SIZE)));
        }

        logger.info("Chunk processed successfully");
        return chunks;
    }

    public int getChunkSize() {
        return CHUNK_SIZE;
    }

    // ================================
    // SAVE CHUNKS (NO EMBEDDINGS TEMP)
    // ================================
    public void saveChunks(Document savedDoc, List<String> chunks) {

        for (String chunkText : chunks) {

            if (chunkText.length() > 3000) {
                chunkText = chunkText.substring(0, 3000);
            }

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(savedDoc);
            chunk.setContent(chunkText);

            // 🔴 Embedding temporarily disabled
            chunk.setEmbedding(null);

            chunkRepository.save(chunk);
        }
    }
}
