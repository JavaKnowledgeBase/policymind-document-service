package com.policymind.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class RecaptchaService {

    private static final Logger logger = LoggerFactory.getLogger(RecaptchaService.class);

    private final RestClient restClient;
    private final boolean enabled;
    private final String secretKey;
    private final double minScore;
    private final String verifyUrl;

    public RecaptchaService(
            @Value("${app.recaptcha.enabled:false}") boolean enabled,
            @Value("${app.recaptcha.secret-key:}") String secretKey,
            @Value("${app.recaptcha.min-score:0.2}") double minScore,
            @Value("${app.recaptcha.verify-url:https://www.google.com/recaptcha/api/siteverify}") String verifyUrl
    ) {
        this.restClient = RestClient.builder().build();
        this.enabled = enabled;
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.minScore = minScore;
        this.verifyUrl = verifyUrl;
    }

    public void verifyOrSkip(String token, String action) {
        if (!enabled) {
            return;
        }

        if (secretKey.isBlank()) {
            throw new IllegalArgumentException("reCAPTCHA is enabled but not configured correctly.");
        }

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("reCAPTCHA verification is required.");
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token.trim());

        Map<String, Object> response = restClient.post()
                .uri(verifyUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            logger.warn("reCAPTCHA verification failed for action='{}', response={}", action, response);
            throw new IllegalArgumentException("reCAPTCHA verification failed. Please try again.");
        }

        String responseAction = stringValue(response.get("action"));
        double score = doubleValue(response.get("score"));

        if (!responseAction.isBlank() && !action.equals(responseAction)) {
            logger.warn("reCAPTCHA action mismatch. expected='{}', actual='{}'", action, responseAction);
            throw new IllegalArgumentException("reCAPTCHA verification did not match the requested action.");
        }

        if (score > 0 && score < minScore) {
            logger.warn("reCAPTCHA score below threshold. action='{}', score={}", action, score);
            throw new IllegalArgumentException("reCAPTCHA score was too low. Please try again.");
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private double doubleValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return value == null ? 0 : Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
