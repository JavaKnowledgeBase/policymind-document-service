package com.policymind.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class DocumentProcessingWorker {

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingWorker.class);

    private final DocumentProcessingPipeline documentProcessingPipeline;

    public DocumentProcessingWorker(DocumentProcessingPipeline documentProcessingPipeline) {
        this.documentProcessingPipeline = documentProcessingPipeline;
    }

    @Async
    public void processDocumentAsync(Long documentId, String fileName, byte[] fileBytes) {
        try {
            // The worker boundary is the first step toward extracting processing into its own service.
            logger.info(
                    "Queued document picked up by async worker, documentId={}, file='{}', sizeBytes={}",
                    documentId,
                    fileName,
                    fileBytes == null ? null : fileBytes.length
            );
            documentProcessingPipeline.processStoredDocument(documentId, fileName, fileBytes);
            logger.info("Async worker finished processing documentId={}", documentId);
        } catch (Exception ex) {
            logger.error("Async document processing failed for documentId={}", documentId, ex);
        }
    }
}
