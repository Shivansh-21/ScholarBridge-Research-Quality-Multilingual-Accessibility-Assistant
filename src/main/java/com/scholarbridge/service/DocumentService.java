package com.scholarbridge.service;

import com.scholarbridge.dao.DocumentDAO;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class DocumentService {
    private final DocumentDAO documentDAO = new DocumentDAO();

    public Map<String, Object> dashboard() throws SQLException {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        dashboard.putAll(documentDAO.dashboardStats());
        dashboard.put("recentUploads", documentDAO.recentUploads(5));
        dashboard.put("recentAudits", documentDAO.recentAudits(5));
        return dashboard;
    }
}
