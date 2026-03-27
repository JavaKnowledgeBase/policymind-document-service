package com.policymind.document.controller;

import com.policymind.document.dto.PolicyComposeRequest;
import com.policymind.document.exception.DocumentProcessingException;
import com.policymind.document.service.PolicyComposeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping
public class PolicyComposeController {

    private static final Logger logger = LoggerFactory.getLogger(PolicyComposeController.class);
    private static final CacheControl NO_STORE = CacheControl.maxAge(0, TimeUnit.SECONDS)
            .noCache()
            .mustRevalidate()
            .cachePrivate()
            .noStore();

    private final PolicyComposeService policyComposeService;

    public PolicyComposeController(PolicyComposeService policyComposeService) {
        this.policyComposeService = policyComposeService;
    }

    @PostMapping("/policy-compose")
    public ResponseEntity<Map<String, Object>> composePolicy(@RequestBody(required = false) PolicyComposeRequest request,
                                                             @RequestHeader(value = "X-Policy-Compose-Request-Id", required = false) String requestId) {
        logger.info("Policy compose request received, requestId='{}', mode='{}', policyType='{}'",
                requestId,
                request == null ? null : request.getMode(),
                request == null ? null : request.getPolicyType());
        return noStore(HttpStatus.OK, policyComposeService.composePolicy(request == null ? new PolicyComposeRequest() : request));
    }

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentProcessingException(DocumentProcessingException ex) {
        logger.error("Policy compose request failed: {}", ex.getMessage(), ex);
        return noStore(HttpStatus.BAD_REQUEST, Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex) {
        logger.error("Unexpected policy compose error", ex);
        return noStore(HttpStatus.INTERNAL_SERVER_ERROR, Map.of("error", "Unexpected server error during policy composition."));
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
