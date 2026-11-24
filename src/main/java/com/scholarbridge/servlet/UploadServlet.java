package com.scholarbridge.servlet;

import com.scholarbridge.dao.*;
import com.scholarbridge.model.*;
import com.scholarbridge.service.*;
import com.scholarbridge.util.FileStorageUtil;
import com.scholarbridge.util.JsonUtil;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.List;

@WebServlet("/api/upload")
@MultipartConfig(maxFileSize=10L*1024*1024, maxRequestSize=11L*1024*1024, fileSizeThreshold=1024*1024)
public class UploadServlet extends HttpServlet {
    private static final long MAX_SIZE = 10L * 1024 * 1024;
    private final FileStorageUtil storage = new FileStorageUtil();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Part file;
        try {
            file = request.getPart("file");
        } catch (Exception exception) {
            JsonUtil.error(response, 400, "A PDF file is required.");
            return;
        }
        if (file == null || file.getSize() == 0) {
            JsonUtil.error(response, 400, "A PDF file is required.");
            return;
        }
        if (file.getSize() > MAX_SIZE) {
            JsonUtil.error(response, 413, "The PDF exceeds the 10 MB maximum.");
            return;
        }
        String originalName = file.getSubmittedFileName();
        String contentType = file.getContentType();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            JsonUtil.error(response, 400, "The file name must end with .pdf.");
            return;
        }
        if (!"application/pdf".equalsIgnoreCase(contentType)
                && !"application/x-pdf".equalsIgnoreCase(contentType)) {
            JsonUtil.error(response, 415, "The uploaded file MIME type is not PDF.");
            return;
        }

        FileStorageUtil.StoredFile stored = null;
        Document document = new Document();
        try {
            stored = storage.store(originalName, contentType, file.getSize(), file.getInputStream());
            document.setOriginalFilename(originalName);
            document.setServerFilename(stored.serverName());
            document.setFilePath(stored.absolutePath().toString());
            document.setFileSize(stored.size());
            document.setMimeType(contentType);
            document.setAnalysisStatus("EXTRACTING");

            PDFExtractionService extraction = new PDFExtractionService();
            extraction.extract(document, stored.absolutePath());

            DocumentDAO documentDAO = new DocumentDAO();
            long id = documentDAO.insert(document);
            document.setId(id);

            ResearchAuditService auditService = new ResearchAuditService();
            ResearchAuditService.AuditReport report = auditService.audit(document);
            for (Issue issue : report.issues()) issue.setDocumentId(id);

            AuditResult audit = new AuditResult();
            audit.setDocumentId(id);
            audit.setQualityScore(report.score());
            audit.setTotalIssues(report.issues().size());
            audit.setCriticalCount(report.criticalCount());
            audit.setMediumCount(report.mediumCount());
            audit.setLowCount(report.lowCount());
            audit.setScoreBreakdown(report.scoreBreakdown());

            new AuditResultDAO().upsert(audit);
            new IssueDAO().replaceForDocument(id, report.issues());
            documentDAO.updateStatus(id, "COMPLETED");

            JsonUtil.json(response, 201,
                    "{\"documentId\":" + id + ",\"qualityScore\":" + report.score()
                    + ",\"issueCount\":" + report.issues().size()
                    + ",\"message\":\"PDF uploaded and analyzed successfully.\"}");
        } catch (IllegalArgumentException | IOException | SQLException exception) {
            if (stored != null) {
                try { Files.deleteIfExists(stored.absolutePath()); } catch (IOException ignored) {}
            }
            JsonUtil.error(response, 422, exception.getMessage() == null
                    ? "The PDF could not be processed." : exception.getMessage());
        }
    }
}
