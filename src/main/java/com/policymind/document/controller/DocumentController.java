
package com.policymind.document.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import com.policymind.document.service.DocumentService;

import java.util.Map;

@RestController
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	// Upload returns immediately and lets the worker finish parsing/chunking/embedding in the background.
	@PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(documentService.submitDocument(file));
    }

    // This gives the UI a polling-friendly status endpoint ahead of a full worker/service split.
    @GetMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> getDocumentStatus(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentStatus(id));
    }
	
	@PostMapping("/{id}/ask")
	public ResponseEntity<Map<String, Object>> askQuestion(
	        @PathVariable Long id,
	        @RequestBody(required = false) Map<String, Object> payload,
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String embeddingProvider,
            @RequestParam(required = false) String answerProvider) {

        String finalQuestion = question;
        if (finalQuestion == null && payload != null && payload.get("question") != null) {
            finalQuestion = payload.get("question").toString();
        }

        if (finalQuestion == null || finalQuestion.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
        }

        String finalEmbeddingProvider = embeddingProvider;
        if ((finalEmbeddingProvider == null || finalEmbeddingProvider.isBlank()) && payload != null && payload.get("embeddingProvider") != null) {
            finalEmbeddingProvider = payload.get("embeddingProvider").toString();
        }

        String finalAnswerProvider = answerProvider;
        if ((finalAnswerProvider == null || finalAnswerProvider.isBlank()) && payload != null && payload.get("answerProvider") != null) {
            finalAnswerProvider = payload.get("answerProvider").toString();
        }

	    return ResponseEntity.ok(documentService.askQuestion(id, finalQuestion, finalEmbeddingProvider, finalAnswerProvider));
	}
    
}
