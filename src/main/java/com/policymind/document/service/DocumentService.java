/**
 * Processes an uploaded document from end to end.
 *
 * This method performs the complete ingestion pipeline:
 * 1. Saves document metadata to the database.
 * 2. Extracts raw text from the uploaded file using PdfService.
 * 3. Splits the extracted text into smaller logical chunks using ChunkService.
 * 4. Generates embeddings for each chunk.
 * 5. Stores chunks and their embeddings in the database.
 * 6. Updates document status to COMPLETED.
 *
 * Why this exists:
 * Large documents cannot be sent directly to an LLM due to token limits.
 * Therefore, we break documents into smaller pieces and prepare them
 * for semantic search using embeddings.
 *
 * @param file The uploaded document file (PDF format expected).
 * @return Status message indicating success or failure of processing.
 */

/**
 * ==============================================================
 * PolicyMind AI - DocumentService
 * ==============================================================
 *
 * This is the core orchestration layer of the PolicyMind system.
 *
 * Responsibilities:
 * - Coordinates document ingestion pipeline.
 * - Manages Retrieval-Augmented Generation (RAG) workflow.
 * - Connects chunking, embeddings, similarity ranking, and LLM response.
 *
 * Architecture Flow:
 *
 * Upload → Extract → Chunk → Embed → Store
 * Ask → Embed Question → Cosine Similarity → Top-K → LLM
 *
 * Design Principles:
 * - Separation of concerns
 * - Service-layer orchestration
 * - AI abstraction via service interfaces
 * - Graceful degradation on external API failures
 *
 * This class does NOT:
 * - Parse PDFs directly
 * - Split text directly
 * - Call OpenAI directly
 *
 * Those are delegated to dedicated services.
 */
package com.policymind.document.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.policymind.document.entity.DocumentChunk;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.model.Document;
import com.policymind.document.repository.DocumentChunkRepository;
import com.policymind.document.repository.DocumentRepository;


@Service
public class DocumentService {

	private final PdfService pdfService;
    private final DocumentRepository repository;
    private final ChunkService chunkService;
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    private final OpenAiService openAiService;
    
    

    // ================================
    // DOCUMENT PROCESSING
    // ================================

    public DocumentService(DocumentRepository repository, ChunkService chunkService,
			DocumentChunkRepository chunkRepository, EmbeddingService embeddingService, OpenAiService openAiService, PdfService pdfService) {
		this.pdfService = pdfService;
		this.repository = repository;
		this.chunkService = chunkService;
		this.chunkRepository = chunkRepository;
		this.embeddingService = embeddingService;
		this.openAiService = openAiService;
	}

	public String processDocument(MultipartFile file) {

        try {

            Document doc = new Document();
            doc.setFileName(file.getOriginalFilename());
            doc.setStatus("PROCESSING");
            doc.setCreatedAt(LocalDateTime.now());

            Document savedDoc = repository.save(doc);

            String text = pdfService.extractText(file);
            List<String> chunks = chunkService.chunkText(text);

            chunkService.saveChunks(savedDoc, chunks);

            savedDoc.setStatus("COMPLETED");
            repository.save(savedDoc);

            return "Document uploaded and processed successfully.";

        } catch (Exception e) {
        	throw new DocumentProcessingException("Failed to process document", e);
        }
    }

    // ================================
    // RAG QUESTION ANSWERING
    // ================================
	
	/**
	 * Answers a user question using Retrieval-Augmented Generation (RAG).
	 *
	 * This method performs the following steps:
	 * 1. Retrieves all stored chunks for the given document.
	 * 2. Generates an embedding vector for the user question.
	 * 3. Compares the question embedding against stored chunk embeddings
	 *    using cosine similarity.
	 * 4. Selects the top-K most semantically relevant chunks.
	 * 5. Builds a contextual prompt from those chunks.
	 * 6. Sends the context and question to the LLM for grounded response.
	 *
	 * Why this exists:
	 * Instead of sending the entire document to the LLM,
	 * we retrieve only the most relevant sections.
	 * This reduces cost, improves accuracy, and minimizes hallucination.
	 *
	 * @param documentId ID of the document being queried.
	 * @param question User's natural language question.
	 * @return AI-generated answer based on relevant document sections.
	 */

    public String askQuestion(Long documentId, String question) {

        // 1️⃣ Retrieve chunks
        List<DocumentChunk> chunks =
                chunkRepository.findByDocumentId(documentId);

        if (chunks.isEmpty()) {
            return "No chunks found for this document.";
        }

        // 2️⃣ Embed question
        String questionEmbeddingJson =
                embeddingService.generateEmbedding(question);

        List<Double> questionEmbedding =
                parseEmbedding(questionEmbeddingJson);

        // 3️⃣ Rank chunks by cosine similarity
        Map<DocumentChunk, Double> similarityScores = new HashMap<>();

        for (DocumentChunk chunk : chunks) {

            if (chunk.getEmbedding() == null) continue;

            List<Double> chunkEmbedding =
                    parseEmbedding(chunk.getEmbedding());

            double similarity =
                    cosineSimilarity(questionEmbedding, chunkEmbedding);

            similarityScores.put(chunk, similarity);
        }

        // 4️⃣ Select Top 3 most relevant chunks
        List<DocumentChunk> topChunks =
                similarityScores.entrySet()
                        .stream()
                        .sorted(Map.Entry.<DocumentChunk, Double>comparingByValue()
                                .reversed())
                        .limit(3)
                        .map(Map.Entry::getKey)
                        .toList();

        // 5️⃣ Build context
        StringBuilder context = new StringBuilder();

        for (DocumentChunk chunk : topChunks) {
            context.append(chunk.getContent()).append("\n\n");
        }

        // 6️⃣ Send to LLM
        return openAiService.askLLM(context.toString(), question);
    }

    // ================================
    // COSINE SIMILARITY
    // ================================
    
    /**
     * Computes cosine similarity between two embedding vectors.
     *
     * Cosine similarity measures how similar two vectors are
     * by calculating the cosine of the angle between them.
     *
     * Result range:
     * 1.0  → identical meaning
     * 0.0  → unrelated
     * -1.0 → opposite meaning
     *
     * Why this exists:
     * Embeddings represent semantic meaning numerically.
     * This method allows us to rank document chunks
     * by semantic closeness to the user's question.
     *
     * @param v1 First embedding vector.
     * @param v2 Second embedding vector.
     * @return Similarity score between 0 and 1.
     */

    private double cosineSimilarity(List<Double> v1, List<Double> v2) {

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.size(); i++) {
            dot += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }

        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ================================
    // EMBEDDING PARSER
    // ================================
    
    /**
     * Extracts numeric embedding vector from OpenAI JSON response.
     *
     * OpenAI embedding API returns a JSON structure containing
     * the embedding inside:
     * data[0].embedding
     *
     * This method parses that JSON and converts it into
     * a List<Double> for mathematical similarity comparison.
     *
     * @param json Raw JSON response from OpenAI embedding API.
     * @return List of double values representing embedding vector.
     */

    private List<Double> parseEmbedding(String json) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);

            JsonNode vector =
                    root.get("data").get(0).get("embedding");

            List<Double> result = new ArrayList<>();

            for (JsonNode val : vector) {
                result.add(val.asDouble());
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse embedding", e);
        }
    }
}
