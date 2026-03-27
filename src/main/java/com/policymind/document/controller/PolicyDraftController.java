package com.policymind.document.controller;

import com.policymind.document.dto.PolicyDraftSaveRequest;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.service.PolicyDraftService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping
public class PolicyDraftController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyDraftController.class);
    private static final CacheControl NO_STORE = CacheControl.maxAge(0, TimeUnit.SECONDS)
            .noCache()
            .mustRevalidate()
            .cachePrivate()
            .noStore();

    private final PolicyDraftService policyDraftService;

    public PolicyDraftController(PolicyDraftService policyDraftService) {
        this.policyDraftService = policyDraftService;
    }

    @PostMapping("/policy-drafts")
    public ResponseEntity<Map<String, Object>> saveDraft(@RequestBody(required = false) PolicyDraftSaveRequest request) {
        logger.info("Policy draft save request received, draftId={}, policyType='{}'",
                request == null ? null : request.getDraftId(),
                request == null ? null : request.getPolicyType());
        return noStore(HttpStatus.OK, policyDraftService.saveDraft(request == null ? new PolicyDraftSaveRequest() : request));
    }

    @GetMapping("/policy-drafts")
    public ResponseEntity<List<Map<String, Object>>> listDrafts() {
        return listNoStore(HttpStatus.OK, policyDraftService.listDrafts());
    }

    @GetMapping("/policy-drafts/{draftId}")
    public ResponseEntity<Map<String, Object>> getDraft(@PathVariable Long draftId) {
        return noStore(HttpStatus.OK, policyDraftService.getDraft(draftId));
    }

    @GetMapping("/policy-drafts/{draftId}/versions")
    public ResponseEntity<List<Map<String, Object>>> listVersions(@PathVariable Long draftId) {
        return listNoStore(HttpStatus.OK, policyDraftService.listDraftVersions(draftId));
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentProcessingException(DocumentProcessingException ex) {
        logger.error("Policy draft request failed: {}", ex.getMessage(), ex);
        return noStore(HttpStatus.BAD_REQUEST, Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex) {
        logger.error("Unexpected policy draft error", ex);
        return noStore(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("error", "Unexpected server error during policy draft handling."));
    }

    private ResponseEntity<Map<String, Object>> noStore(HttpStatus status, Map<String, Object> body) {
        return ResponseEntity.status(status)
                .cacheControl(NO_STORE)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("Vary", "Authorization")
                .body(body);
    }

    private ResponseEntity<List<Map<String, Object>>> listNoStore(HttpStatus status, List<Map<String, Object>> body) {
        return ResponseEntity.status(status)
                .cacheControl(NO_STORE)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("Vary", "Authorization")
                .body(body);
    }
}
