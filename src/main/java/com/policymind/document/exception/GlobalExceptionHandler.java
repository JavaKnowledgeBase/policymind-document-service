package com.policymind.document.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Catches whatever a controller-local {@code @ExceptionHandler} does not, so no endpoint can
 * leak a raw stack trace or an inconsistent error shape to the client. Controllers that already
 * define their own handlers (DocumentController, PolicyComposeController, PolicyDraftController)
 * take precedence over this for their own requests; this is the safety net for everything else
 * (AuthController, HeaderContentController, framework-level failures such as malformed JSON,
 * oversized uploads, or missing parameters).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final CacheControl NO_STORE = CacheControl.maxAge(0, TimeUnit.SECONDS)
            .noCache()
            .mustRevalidate()
            .cachePrivate()
            .noStore();

    @ExceptionHandler(DocumentProcessingException.class)
    public ResponseEntity<Map<String, Object>> handleDocumentProcessingException(DocumentProcessingException ex) {
        logger.warn("Request rejected: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Request rejected: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        logger.warn("Dependency unavailable: {}", ex.getMessage());
        return error(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        logger.warn("Request conflicted with existing data", ex);
        return error(HttpStatus.CONFLICT, "The request conflicts with existing data.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        logger.warn("Upload rejected: file too large");
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "The uploaded file is too large.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableBody(HttpMessageNotReadableException ex) {
        logger.warn("Request rejected: malformed request body");
        return error(HttpStatus.BAD_REQUEST, "The request body is missing or malformed.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParameter(MissingServletRequestParameterException ex) {
        logger.warn("Request rejected: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Required parameter '" + ex.getParameterName() + "' is missing.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        logger.warn("Request rejected: {}", ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Parameter '" + ex.getName() + "' has an invalid value.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationFailure(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("Request validation failed.");
        logger.warn("Request rejected: {}", message);
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpectedException(Exception ex) {
        logger.error("Unexpected server error", ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error. Please try again.");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .cacheControl(NO_STORE)
                .header("Pragma", "no-cache")
                .header("Expires", "0")
                .header("Vary", "Authorization")
                .body(Map.of("error", message == null || message.isBlank() ? status.getReasonPhrase() : message));
    }
}
