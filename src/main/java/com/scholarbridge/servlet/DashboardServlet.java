package com.scholarbridge.servlet;

import com.scholarbridge.service.DocumentService;
import com.scholarbridge.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Map<String,Object> data = new DocumentService().dashboard();
            StringBuilder json = new StringBuilder("{");
            json.append("\"totalDocuments\":").append(data.get("totalDocuments"))
                .append(",\"averageScore\":").append(data.get("averageScore"))
                .append(",\"totalIssues\":").append(data.get("totalIssues"))
                .append(",\"recentUploads\":[");
            appendRecentUploads(json, data);
            json.append("],\"recentAudits\":[");
            appendRecentAudits(json, data);
            json.append("]}");
            JsonUtil.json(response, 200, json.toString());
        } catch (SQLException exception) {
            JsonUtil.error(response, 503, "Database unavailable. Verify MySQL and DB environment variables.");
        }
    }

    @SuppressWarnings("unchecked")
    private void appendRecentUploads(StringBuilder json, Map<String,Object> data) {
        List<Map<String,Object>> rows = (List<Map<String,Object>>) data.get("recentUploads");
        for (int i=0;i<rows.size();i++) {
            if (i>0) json.append(',');
            Map<String,Object> row=rows.get(i);
            json.append("{\"id\":").append(row.get("id"))
                .append(",\"name\":\"").append(JsonUtil.escape(String.valueOf(row.get("name"))))
                .append("\",\"uploadDate\":\"").append(JsonUtil.escape(String.valueOf(row.get("uploadDate")))).append("\"}");
        }
    }

    @SuppressWarnings("unchecked")
    private void appendRecentAudits(StringBuilder json, Map<String,Object> data) {
        List<Map<String,Object>> rows = (List<Map<String,Object>>) data.get("recentAudits");
        for (int i=0;i<rows.size();i++) {
            if (i>0) json.append(',');
            Map<String,Object> row=rows.get(i);
            json.append("{\"name\":\"").append(JsonUtil.escape(String.valueOf(row.get("name"))))
                .append("\",\"qualityScore\":").append(row.get("qualityScore"))
                .append(",\"issueCount\":").append(row.get("issueCount"))
                .append(",\"createdAt\":\"").append(JsonUtil.escape(String.valueOf(row.get("createdAt")))).append("\"}");
        }
    }
}
