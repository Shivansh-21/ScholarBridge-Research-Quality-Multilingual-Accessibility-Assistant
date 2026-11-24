package com.scholarbridge.dao;

import com.scholarbridge.config.DatabaseConfig;
import com.scholarbridge.model.Document;

import java.sql.*;
import java.util.*;

public class DocumentDAO {
    public long insert(Document document) throws SQLException {
        String sql = "INSERT INTO documents (original_filename,server_filename,file_path,file_size,mime_type,title,"
                + "authors,abstract_text,doi,keywords,references_text,extracted_text,analysis_status) "
                + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setInsert(statement, document);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("Document insert did not return an ID");
            }
        }
    }

    private void setInsert(PreparedStatement statement, Document document) throws SQLException {
        statement.setString(1, document.getOriginalFilename());
        statement.setString(2, document.getServerFilename());
        statement.setString(3, document.getFilePath());
        statement.setLong(4, document.getFileSize());
        statement.setString(5, document.getMimeType());
        statement.setString(6, document.getTitle());
        statement.setString(7, document.getAuthors());
        statement.setString(8, document.getAbstractText());
        statement.setString(9, document.getDoi());
        statement.setString(10, document.getKeywords());
        statement.setString(11, document.getReferencesText());
        statement.setString(12, document.getExtractedText());
        statement.setString(13, document.getAnalysisStatus());
    }

    public Optional<Document> findById(long id) throws SQLException {
        String sql = "SELECT d.*, COALESCE(a.quality_score,0) AS quality_score "
                + "FROM documents d LEFT JOIN audit_results a ON a.document_id=d.id WHERE d.id=?";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(map(result)) : Optional.empty();
            }
        }
    }

    public List<Map<String,Object>> listAll() throws SQLException {
        String sql = "SELECT d.id,d.original_filename,d.upload_date,d.analysis_status,"
                + "COALESCE(a.quality_score,0) quality_score,"
                + "(SELECT COUNT(*) FROM issues i WHERE i.document_id=d.id) issue_count "
                + "FROM documents d LEFT JOIN audit_results a ON a.document_id=d.id "
                + "ORDER BY d.upload_date DESC";
        List<Map<String,Object>> rows = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                Map<String,Object> row = new LinkedHashMap<>();
                row.put("id", result.getLong("id"));
                row.put("name", result.getString("original_filename"));
                row.put("uploadDate", String.valueOf(result.getTimestamp("upload_date")));
                row.put("qualityScore", result.getInt("quality_score"));
                row.put("issueCount", result.getInt("issue_count"));
                row.put("status", result.getString("analysis_status"));
                rows.add(row);
            }
        }
        return rows;
    }

    public boolean delete(long id) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement find = connection.prepareStatement("SELECT server_filename FROM documents WHERE id=?");
                 PreparedStatement delete = connection.prepareStatement("DELETE FROM documents WHERE id=?")) {
                find.setLong(1, id);
                String serverName = null;
                try (ResultSet result = find.executeQuery()) {
                    if (result.next()) serverName = result.getString(1);
                }
                if (serverName == null) {
                    connection.rollback();
                    return false;
                }
                delete.setLong(1, id);
                delete.executeUpdate();
                connection.commit();
                return true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void updateStatus(long id, String status) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE documents SET analysis_status=? WHERE id=?")) {
            statement.setString(1, status);
            statement.setLong(2, id);
            statement.executeUpdate();
        }
    }

    public Map<String,Object> dashboardStats() throws SQLException {
        Map<String,Object> stats = new LinkedHashMap<>();
        String sql = "SELECT (SELECT COUNT(*) FROM documents) total,"
                + "(SELECT COALESCE(AVG(quality_score),0) FROM audit_results) average_score,"
                + "(SELECT COUNT(*) FROM issues) total_issues";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            if (result.next()) {
                stats.put("totalDocuments", result.getInt("total"));
                stats.put("averageScore", result.getInt("average_score"));
                stats.put("totalIssues", result.getInt("total_issues"));
            }
        }
        return stats;
    }

    public List<Map<String,Object>> recentUploads(int limit) throws SQLException {
        List<Map<String,Object>> resultRows = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id,original_filename,upload_date FROM documents ORDER BY upload_date DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    row.put("id", result.getLong("id"));
                    row.put("name", result.getString("original_filename"));
                    row.put("uploadDate", String.valueOf(result.getTimestamp("upload_date")));
                    resultRows.add(row);
                }
            }
        }
        return resultRows;
    }

    public List<Map<String,Object>> recentAudits(int limit) throws SQLException {
        List<Map<String,Object>> resultRows = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT d.original_filename,a.quality_score,a.total_issues,a.created_at "
                     + "FROM audit_results a JOIN documents d ON d.id=a.document_id "
                     + "ORDER BY a.created_at DESC LIMIT ?")) {
            statement.setInt(1, limit);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    row.put("name", result.getString("original_filename"));
                    row.put("qualityScore", result.getInt("quality_score"));
                    row.put("issueCount", result.getInt("total_issues"));
                    row.put("createdAt", String.valueOf(result.getTimestamp("created_at")));
                    resultRows.add(row);
                }
            }
        }
        return resultRows;
    }

    private Document map(ResultSet result) throws SQLException {
        Document document = new Document();
        document.setId(result.getLong("id"));
        document.setOriginalFilename(result.getString("original_filename"));
        document.setServerFilename(result.getString("server_filename"));
        document.setFilePath(result.getString("file_path"));
        document.setFileSize(result.getLong("file_size"));
        document.setMimeType(result.getString("mime_type"));
        Timestamp upload = result.getTimestamp("upload_date");
        if (upload != null) document.setUploadDate(upload.toLocalDateTime());
        document.setTitle(result.getString("title"));
        document.setAuthors(result.getString("authors"));
        document.setAbstractText(result.getString("abstract_text"));
        document.setDoi(result.getString("doi"));
        document.setKeywords(result.getString("keywords"));
        document.setReferencesText(result.getString("references_text"));
        document.setExtractedText(result.getString("extracted_text"));
        document.setAnalysisStatus(result.getString("analysis_status"));
        return document;
    }
}
