/**
 * Extracts readable text from an uploaded PDF file.
 *
 * Uses Apache PDFBox to convert PDF binary content
 * into plain text format.
 *
 * This is the first step of document ingestion pipeline.
 *
 * @param file Uploaded PDF file.
 * @return Extracted plain text from the document.
 */

/**
 * ==============================================================
 * PolicyMind AI - PdfService
 * ==============================================================
 *
 * Extracts readable text from uploaded PDF files.
 *
 * This service isolates file-format handling logic
 * from the core business logic layer.
 *
 * Designed for future extensibility:
 * - PDF (current)
 * - DOCX (future)
 * - TXT (future)
 */
package com.policymind.document.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);
	
	private String status;

	public String getStatus() { return status; }
	public void setStatus(String status) { this.status = status; }

    public String extractText(MultipartFile file) throws IOException {
        return extractText(file.getBytes());
    }

    public String extractText(byte[] fileBytes) throws IOException {
        logger.info("PdfService.extractText called, sizeBytes={}", fileBytes == null ? null : fileBytes.length);
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(fileBytes))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            logger.info("PdfService.extractText completed, pages={}, extractedCharacters={}", document.getNumberOfPages(), text == null ? null : text.length());
            return text;
        } catch (IOException ex) {
            logger.error("PdfService.extractText failed", ex);
            throw ex;
        }
    }
}
