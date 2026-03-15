package com.policymind.document.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    private static final String HEALTH_MESSAGE = "PolicyMind Document Service is running";

    @GetMapping({"/", "/health"})
    public String home() {
        return HEALTH_MESSAGE;
    }

}
