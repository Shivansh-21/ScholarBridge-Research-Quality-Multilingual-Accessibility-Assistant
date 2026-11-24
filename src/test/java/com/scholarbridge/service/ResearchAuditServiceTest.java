package com.scholarbridge.service;

import com.scholarbridge.model.Document;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ResearchAuditServiceTest {
    private final ResearchAuditService service = new ResearchAuditService();

    @Test
    void missingMetadataProducesDeductionsAndDoesNotGoBelowZero() {
        Document document = new Document();
        DocumentAuditResult result = new DocumentAuditResult(service.audit(document).score(),
                service.audit(document).issues().size());
        assertEquals(10, result.score());
        assertTrue(result.issueCount() >= 7);
    }

    @Test
    void completeMetadataProducesHighScore() {
        Document document = new Document();
        document.setTitle("A Complete Research Study");
        document.setAuthors("Jane Doe and John Smith");
        document.setAbstractText("This abstract has sufficient detail to describe the study and its findings.");
        document.setKeywords("research, methods, data");
        document.setDoi("10.1234/example.2026");
        document.setReferencesText("References\n[1] Author One. Journal Name. 2024. A complete reference entry.\n"
                + "[2] Author Two. Conference Proceedings. 2025. Another complete entry.\n"
                + "[3] Author Three. Book Title. 2023. A third complete entry.");
        document.setExtractedText("Introduction\nMethods\nResults\nDiscussion\nConclusion");
        ResearchAuditService.AuditReport report = service.audit(document);
        assertEquals(100, report.score(), report.issues().stream()
                .map(issue -> issue.getCategory()).toList().toString());
        assertEquals(0, report.issues().size());
    }

    @Test
    void incompleteReferencesAreDetected() {
        Document document = new Document();
        document.setTitle("Title");
        document.setAuthors("Author");
        document.setAbstractText("A sufficiently detailed abstract.");
        document.setKeywords("keyword");
        document.setDoi("10.1234/test");
        document.setReferencesText("Only one reference");
        document.setExtractedText("Introduction Methods Results Discussion Conclusion");
        assertTrue(service.audit(document).issues().stream()
                .anyMatch(issue -> "References".equals(issue.getCategory())));
    }

    private record DocumentAuditResult(int score, int issueCount) {}
}
