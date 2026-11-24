package com.scholarbridge.servlet;

import com.scholarbridge.dao.*;
import com.scholarbridge.model.*;
import com.scholarbridge.util.FileStorageUtil;
import com.scholarbridge.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/api/documents/*")
public class DocumentServlet extends HttpServlet {
    private final DocumentDAO documentDAO = new DocumentDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        try {
            if (path == null || "/".equals(path)) {
                List<Map<String,Object>> documents = documentDAO.listAll();
                StringBuilder json = new StringBuilder("[");
                for (int i=0;i<documents.size();i++) {
                    if (i>0) json.append(',');
                    Map<String,Object> d=documents.get(i);
                    json.append("{\"id\":").append(d.get("id"))
                        .append(",\"name\":\"").append(JsonUtil.escape(String.valueOf(d.get("name"))))
                        .append("\",\"uploadDate\":\"").append(JsonUtil.escape(String.valueOf(d.get("uploadDate"))))
                        .append("\",\"qualityScore\":").append(d.get("qualityScore"))
                        .append(",\"issueCount\":").append(d.get("issueCount"))
                        .append(",\"status\":\"").append(JsonUtil.escape(String.valueOf(d.get("status")))).append("\"}");
                }
                json.append(']');
                JsonUtil.json(response,200,json.toString());
                return;
            }
            long id = parseId(path);
            Optional<Document> found = documentDAO.findById(id);
            if (found.isEmpty()) {
                JsonUtil.error(response,404,"Document not found.");
                return;
            }
            Document d=found.get();
            List<Issue> issues=new IssueDAO().findByDocumentId(id);
            Optional<AuditResult> audit=new AuditResultDAO().findByDocumentId(id);
            StringBuilder json=new StringBuilder("{");
            appendDocument(json,d);
            json.append(",\"audit\":");
            if (audit.isPresent()) {
                AuditResult a=audit.get();
                json.append("{\"qualityScore\":").append(a.getQualityScore())
                    .append(",\"totalIssues\":").append(a.getTotalIssues())
                    .append(",\"criticalCount\":").append(a.getCriticalCount())
                    .append(",\"mediumCount\":").append(a.getMediumCount())
                    .append(",\"lowCount\":").append(a.getLowCount())
                    .append(",\"scoreBreakdown\":\"").append(JsonUtil.escape(a.getScoreBreakdown())).append("\"}");
            } else json.append("null");
            json.append(",\"issues\":[");
            for(int i=0;i<issues.size();i++){
                if(i>0)json.append(',');
                Issue issue=issues.get(i);
                json.append("{\"severity\":\"").append(JsonUtil.escape(issue.getSeverity()))
                    .append("\",\"category\":\"").append(JsonUtil.escape(issue.getCategory()))
                    .append("\",\"recommendation\":\"").append(JsonUtil.escape(issue.getRecommendation())).append("\"}");
            }
            json.append("]}");
            JsonUtil.json(response,200,json.toString());
        } catch (NumberFormatException exception) {
            JsonUtil.error(response,400,"Invalid document ID.");
        } catch (SQLException exception) {
            JsonUtil.error(response,503,"Database unavailable.");
        }
    }

    private void appendDocument(StringBuilder json, Document d) {
        json.append("\"id\":").append(d.getId())
            .append(",\"name\":\"").append(JsonUtil.escape(d.getOriginalFilename()))
            .append("\",\"uploadDate\":\"").append(d.getUploadDate()==null?"":d.getUploadDate()).append("\"")
            .append(",\"title\":\"").append(JsonUtil.escape(d.getTitle())).append("\"")
            .append(",\"authors\":\"").append(JsonUtil.escape(d.getAuthors())).append("\"")
            .append(",\"abstractText\":\"").append(JsonUtil.escape(d.getAbstractText())).append("\"")
            .append(",\"doi\":\"").append(JsonUtil.escape(d.getDoi())).append("\"")
            .append(",\"keywords\":\"").append(JsonUtil.escape(d.getKeywords())).append("\"")
            .append(",\"referencesText\":\"").append(JsonUtil.escape(d.getReferencesText())).append("\"")
            .append(",\"status\":\"").append(JsonUtil.escape(d.getAnalysisStatus())).append("\"");
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            long id=parseId(request.getPathInfo());
            Optional<Document> document=documentDAO.findById(id);
            if(document.isEmpty()){JsonUtil.error(response,404,"Document not found.");return;}
            if(!documentDAO.delete(id)){JsonUtil.error(response,404,"Document not found.");return;}
            new FileStorageUtil().delete(document.get().getServerFilename());
            JsonUtil.json(response,200,"{\"message\":\"Document deleted successfully.\"}");
        } catch(NumberFormatException exception){JsonUtil.error(response,400,"Invalid document ID.");}
        catch(SQLException exception){JsonUtil.error(response,503,"Database unavailable.");}
        catch(IOException exception){JsonUtil.error(response,500,"Database record deleted, but uploaded file cleanup failed.");}
    }

    private long parseId(String path) {
        if(path==null||path.length()<2)throw new NumberFormatException();
        return Long.parseLong(path.substring(1).split("/")[0]);
    }
}
