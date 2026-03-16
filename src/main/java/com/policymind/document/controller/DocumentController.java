
package com.policymind.document.controller;

import com.policymind.document.exception.DocumentProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.web.multipart.MultipartFile;
import com.policymind.document.service.DocumentService;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private static final CacheControl NO_STORE = CacheControl.maxAge(0, TimeUnit.SECONDS)
            .noCache()
            .mustRevalidate()
            .cachePrivate()
            .noStore();

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	// Upload returns immediately and lets the worker finish parsing/chunking/embedding in the background.
	@PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file) {
        logger.info(
                "Upload request received, fileName='{}', contentType='{}', sizeBytes={}",
                file == null ? null : file.getOriginalFilename(),
                file == null ? null : file.getContentType(),
                file == null ? null : file.getSize()
        );
        return noStore(HttpStatus.ACCEPTED, documentService.submitDocument(file));
    }

    // This gives the UI a polling-friendly status endpoint ahead of a full worker/service split.
    @GetMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> getDocumentStatus(@PathVariable Long id) {
        return noStore(HttpStatus.OK, documentService.getDocumentStatus(id));
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
            return noStore(HttpStatus.BAD_REQUEST, Map.of("error", "Question is required"));
        }

        String finalEmbeddingProvider = embeddingProvider;
        if ((finalEmbeddingProvider == null || finalEmbeddingProvider.isBlank()) && payload != null && payload.get("embeddingProvider") != null) {
            finalEmbeddingProvider = payload.get("embeddingProvider").toString();
        }

        String finalAnswerProvider = answerProvider;
        if ((finalAnswerProvider == null || finalAnswerProvider.isBlank()) && payload != null && payload.get("answerProvider") != null) {
            finalAnswerProvider = payload.get("answerProvider").toString();
        }

	    return noStore(HttpStatus.OK, documentService.askQuestion(id, finalQuestion, finalEmbeddingProvider, finalAnswerProvider));
	}

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentProcessingException(DocumentProcessingException ex) {
        logger.error("Document request failed: {}", ex.getMessage(), ex);
        return noStore(HttpStatus.BAD_REQUEST, Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex) {
        logger.error("Unexpected document controller error", ex);
        return noStore(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("error", "Unexpected server error during document handling."));
    }

    private ResponseEntity<Map<String, Object>> noStore(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status)
                .cacheControl(NO_STORE)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("Vary", "Authorization")
                .body(body);
    }
}
