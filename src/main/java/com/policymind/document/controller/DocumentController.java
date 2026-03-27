package com.policymind.document.controller;

import com.policymind.document.dto.AnalysisJobRequest;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.service.AnalysisJobService;
import com.policymind.document.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping
public class DocumentController {

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
    private static final CacheControl NO_STORE = CacheControl.maxAge(0, TimeUnit.SECONDS)
            .noCache()
            .mustRevalidate()
            .cachePrivate()
            .noStore();

    private final DocumentService documentService;
    private final AnalysisJobService analysisJobService;

    public DocumentController(DocumentService documentService,
                              AnalysisJobService analysisJobService) {
        this.documentService = documentService;
        this.analysisJobService = analysisJobService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                      @RequestHeader(value = "X-Upload-Request-Id", required = false) String uploadRequestId,
                                                      HttpServletRequest request) {
        logger.info(
                "Upload request received, uploadRequestId='{}', path='{}', remoteAddr='{}', fileName='{}', contentType='{}', sizeBytes= {}",
                uploadRequestId,
                request == null ? null : request.getRequestURI(),
                request == null ? null : request.getRemoteAddr(),
                file == null ? null : file.getOriginalFilename(),
                file == null ? null : file.getContentType(),
                file == null ? null : file.getSize()
        );
        return noStore(HttpStatus.ACCEPTED, documentService.submitDocument(file));
    }

    @GetMapping("/documents/{id}")
    public ResponseEntity<Map<String, Object>> getDocumentStatus(@PathVariable Long id) {
        return noStore(HttpStatus.OK, documentService.getDocumentStatus(id));
    }

    @PostMapping("/{id}/ask")
    public ResponseEntity<Map<String, Object>> askQuestion(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "X-Question-Request-Id", required = false) String questionRequestId,
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

        logger.info(
                "Question request received, questionRequestId='{}', documentId={}, embeddingProvider='{}', answerProvider='{}'",
                questionRequestId,
                id,
                embeddingProvider,
                answerProvider
        );

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

    @PostMapping("/documents/{id}/analysis-jobs")
    public ResponseEntity<Map<String, Object>> createAnalysisJob(@PathVariable Long id,
                                                                 @RequestBody(required = false) AnalysisJobRequest request,
                                                                 @RequestHeader(value = "X-Analysis-Request-Id", required = false) String analysisRequestId) {
        logger.info("Analysis job request received, analysisRequestId='{}', documentId={}", analysisRequestId, id);
        return noStore(HttpStatus.ACCEPTED, analysisJobService.createQuestionAnswerJob(id, request == null ? new AnalysisJobRequest() : request));
    }

    @GetMapping("/analysis-jobs/{jobId}")
    public ResponseEntity<Map<String, Object>> getAnalysisJobStatus(@PathVariable Long jobId) {
        return noStore(HttpStatus.OK, analysisJobService.getJobStatus(jobId));
    }

    @GetMapping("/analysis-jobs/{jobId}/result")
    public ResponseEntity<Map<String, Object>> getAnalysisJobResult(@PathVariable Long jobId) {
        return noStore(HttpStatus.OK, analysisJobService.getJobResult(jobId));
    }

    @GetMapping("/analysis-jobs/{jobId}/trace")
    public ResponseEntity<Map<String, Object>> getAnalysisJobTrace(@PathVariable Long jobId) {
        return noStore(HttpStatus.OK, analysisJobService.getJobTrace(jobId));
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
