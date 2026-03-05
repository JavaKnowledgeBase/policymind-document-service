package com.policymind.document.controller;

import com.policymind.document.service.HeaderContentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/content")
public class HeaderContentController {

    private final HeaderContentService headerContentService;

    public HeaderContentController(HeaderContentService headerContentService) {
        this.headerContentService = headerContentService;
    }

    @GetMapping("/header-lines")
    public ResponseEntity<Map<String, Object>> getHeaderLines() {
        List<String> items = headerContentService.getHeaderLines();
        return ResponseEntity.ok(
                Map.of(
                        "items", items,
                        "count", items.size()
                )
        );
    }
}
