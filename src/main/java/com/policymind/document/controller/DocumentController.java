
package com.policymind.document.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import com.policymind.document.service.DocumentService;

@RestController
//@RequestMapping("/documents")
//@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    
    

    public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}



	@PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.processDocument(file));
    }
	
	@PostMapping("/{id}/ask")
	public ResponseEntity<String> askQuestion(
	        @PathVariable Long id,
	        @RequestBody String question) {

	    return ResponseEntity.ok(documentService.askQuestion(id, question));
	}
    
}
