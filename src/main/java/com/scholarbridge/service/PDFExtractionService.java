package com.scholarbridge.service;

import com.scholarbridge.model.Document;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFExtractionService {
    private final DOIValidator doiValidator = new DOIValidator();

    public void extract(Document document, Path pdfPath) throws IOException {
        try (PDDocument pdf = PDDocument.load(pdfPath.toFile())) {
            if (pdf.isEncrypted()) throw new IOException("Encrypted PDFs are not supported");
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(pdf);
            if (text == null || text.trim().isEmpty()) throw new IOException("The PDF contains no extractable text");
            text = text.replace('\u00a0', ' ').trim();
            document.setExtractedText(text);
            document.setTitle(detectTitle(text));
            document.setAuthors(detectAuthors(text));
            document.setAbstractText(detectAbstract(text));
            document.setDoi(doiValidator.extract(text));
            document.setKeywords(detectKeywords(text));
            document.setReferencesText(detectReferences(text));
        } catch (org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException exception) {
            throw new IOException("The PDF is encrypted or password protected", exception);
        }
    }

    public String detectTitle(String text) {
        if (text == null) return null;
        String[] lines = text.split("\\R");
        for (int i = 0; i < Math.min(lines.length, 25); i++) {
            String line = lines[i].trim();
            if (line.length() >= 12 && line.length() <= 250
                    && !line.matches("(?i).*(abstract|keywords|doi|page|journal|university).*")
                    && Character.isLetterOrDigit(line.charAt(0))) return line;
        }
        return null;
    }

    public String detectAuthors(String text) {
        if (text == null) return null;
        String[] lines = text.split("\\R");
        for (int i = 1; i < Math.min(lines.length, 15); i++) {
            String line = lines[i].trim();
            if (line.matches(".*\\b[A-Z][a-z]+(?:[- ][A-Z][A-Za-z.]+)*(?:\\s*,\\s*.*)?\\b.*")
                    && line.length() >= 3 && line.length() <= 300
                    && !line.matches("(?i).*\\b(abstract|keywords|doi|department|university|journal)\\b.*")) {
                return line;
            }
        }
        return null;
    }

    public String detectAbstract(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile(
                "(?is)\\b(?:abstract|summary)\\b\\s*[:.-]?\\s*(.{80,5000}?)(?=\\n\\s*(?:keywords|key\\s*words|index\\s*terms|1\\s*[.]?\\s*introduction)|\\z)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    public String detectKeywords(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile(
                "(?is)\\b(?:keywords?|key\\s*words|index\\s*terms)\\b\\s*[:.-]?\\s*(.{3,600}?)(?=\\n|\\z)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    public String detectReferences(String text) {
        if (text == null) return null;
        Pattern pattern = Pattern.compile(
                "(?is)\\b(?:references|bibliography|works\\s*cited)\\b\\s*(.{100,100000})");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? clean(matcher.group(1)) : null;
    }

    private String clean(String value) {
        return value == null ? null : value.replaceAll("\\s+", " ").trim();
    }
}
