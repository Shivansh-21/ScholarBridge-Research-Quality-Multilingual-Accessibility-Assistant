package com.scholarbridge.service;

import com.scholarbridge.model.Document;
import com.scholarbridge.model.Issue;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ResearchAuditService {
    private final DOIValidator doiValidator = new DOIValidator();

    public AuditReport audit(Document document) {
        List<Issue> issues = new ArrayList<>();
        int score = 100;

        score -= add(document.getTitle(), "CRITICAL", "Title",
                "Add a clear, descriptive research title.", 15, issues);
        score -= add(document.getAuthors(), "HIGH", "Authors",
                "List the full names and affiliations of all authors.", 10, issues);
        score -= add(document.getAbstractText(), "CRITICAL", "Abstract",
                "Add a structured abstract summarizing the study, method and findings.", 15, issues);
        score -= add(document.getKeywords(), "MEDIUM", "Keywords",
                "Add relevant keywords or index terms.", 10, issues);

        if (document.getDoi() == null || document.getDoi().isBlank()) {
            issues.add(issue(document, "MEDIUM", "DOI",
                    "Add a DOI where available. Absence does not mean the paper is unregistered."));
            score -= 10;
        } else if (!doiValidator.isValidFormat(document.getDoi())) {
            issues.add(issue(document, "HIGH", "DOI",
                    "Correct the DOI format. This check validates format only, not DOI registration."));
            score -= 10;
        }

        if (document.getReferencesText() == null || document.getReferencesText().length() < 100
                || countReferences(document.getReferencesText()) < 3) {
            issues.add(issue(document, "MEDIUM", "References",
                    "Include at least several complete, consistently formatted reference entries."));
            score -= 10;
        }

        String text = document.getExtractedText() == null ? "" : document.getExtractedText().toLowerCase();
        int basicSections = 0;
        for (String section : new String[]{"introduction", "method", "results", "discussion", "conclusion"}) {
            if (text.contains(section)) basicSections++;
        }
        if (basicSections < 4) {
            issues.add(issue(document, "CRITICAL", "Document Sections",
                    "Add clearly labeled introduction, methods, results, discussion and conclusion sections."));
            score -= 20;
        }

        score = Math.max(0, score);
        int critical = (int) issues.stream().filter(i -> i.getSeverity().equals("CRITICAL") || i.getSeverity().equals("HIGH")).count();
        int medium = (int) issues.stream().filter(i -> i.getSeverity().equals("MEDIUM")).count();
        int low = (int) issues.stream().filter(i -> i.getSeverity().equals("LOW")).count();
        String breakdown = "Start 100; Missing title -15; Missing authors -10; Missing abstract -15; "
                + "Missing keywords -10; Missing DOI -10; Malformed DOI -10; "
                + "Incomplete references -10; Missing basic sections -20; minimum 0.";
        return new AuditReport(score, issues, critical, medium, low, breakdown);
    }

    private int add(String value, String severity, String category, String recommendation,
                    int deduction, List<Issue> issues) {
        if (value == null || value.isBlank()) {
            issues.add(issue(null, severity, category, recommendation));
            return deduction;
        }
        return 0;
    }

    private Issue issue(Document document, String severity, String category, String recommendation) {
        Issue issue = new Issue();
        if (document != null) issue.setDocumentId(document.getId());
        issue.setSeverity(severity);
        issue.setCategory(category);
        issue.setRecommendation(recommendation);
        return issue;
    }

    private int countReferences(String references) {
        if (references == null) return 0;
        Pattern pattern = Pattern.compile("(?m)^\\s*(?:\\[\\d{1,3}\\]\\s+|\\d{1,3}[.)]\\s+|[^\\n]{20,}\\b(?:19|20)\\d{2}\\b)");
        return (int) pattern.matcher(references).results().count();
    }

    public record AuditReport(int score, List<Issue> issues, int criticalCount,
                              int mediumCount, int lowCount, String scoreBreakdown) {}
}
