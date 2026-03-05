package com.policymind.document.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class PdfServiceTest {

    @Test
    public void extractText_readsPdfContent() throws Exception {
        // create a simple PDF in memory
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage();
        doc.addPage(page);
        PDPageContentStream cs = new PDPageContentStream(doc, page);
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(100, 700);
        cs.showText("Hello PDF Test");
        cs.endText();
        cs.close();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        doc.save(baos);
        doc.close();

        MockMultipartFile multipart = new MockMultipartFile("file", "test.pdf", "application/pdf", baos.toByteArray());

        PdfService svc = new PdfService();
        String text = svc.extractText(multipart);
        assertNotNull(text);
        assertTrue(text.contains("Hello PDF Test"));
    }
}
