
package com.policymind.document.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.policymind.document.service.DocumentService;

import java.util.Map;

@RestController
//@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;
    
    

    public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}



	@PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.processDocument(file));
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
