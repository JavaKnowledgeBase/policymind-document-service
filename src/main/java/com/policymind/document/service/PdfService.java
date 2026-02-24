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

import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class PdfService {
    public String extractText(MultipartFile file) throws IOException {
        PDDocument document = PDDocument.load(file.getInputStream());
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }
}
