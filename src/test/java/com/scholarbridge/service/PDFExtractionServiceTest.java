package com.scholarbridge.service;

import com.scholarbridge.model.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PDFExtractionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void extractsTextFromValidPDF() throws Exception {
        Path pdf = tempDir.resolve("valid.pdf");
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                stream.beginText();
                stream.setFont(PDType1Font.HELVETICA_BOLD, 14);
                stream.newLineAtOffset(50, 700);
                stream.showText("A Research Study");
                stream.endText();
            }
            document.save(pdf.toFile());
        }
        Document model = new Document();
        new PDFExtractionService().extract(model, pdf);
        assertTrue(model.getExtractedText().contains("A Research Study"));
    }

    @Test
    void rejectsInvalidPDF() throws Exception {
        Path pdf = tempDir.resolve("invalid.pdf");
        Files.writeString(pdf, "This is not a PDF");
        assertThrows(Exception.class,
                () -> new PDFExtractionService().extract(new Document(), pdf));
    }

    @Test
    void rejectsEmptyPDF() throws Exception {
        Path pdf = tempDir.resolve("empty.pdf");
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            document.save(pdf.toFile());
        }
        assertThrows(Exception.class,
                () -> new PDFExtractionService().extract(new Document(), pdf));
    }
}
