package com.scholarbridge.dao;

import com.scholarbridge.config.DatabaseConfig;
import com.scholarbridge.model.Issue;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class IssueDAO {
    public void replaceForDocument(long documentId, List<Issue> issues) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM issues WHERE document_id=?");
                 PreparedStatement insert = connection.prepareStatement(
                         "INSERT INTO issues (document_id,severity,category,recommendation) VALUES (?,?,?,?)")) {
                delete.setLong(1, documentId);
                delete.executeUpdate();
                for (Issue issue : issues) {
                    insert.setLong(1, documentId);
                    insert.setString(2, issue.getSeverity());
                    insert.setString(3, issue.getCategory());
                    insert.setString(4, issue.getRecommendation());
                    insert.addBatch();
                }
                if (!issues.isEmpty()) insert.executeBatch();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    public List<Issue> findByDocumentId(long documentId) throws SQLException {
        List<Issue> issues = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM issues WHERE document_id=? ORDER BY id")) {
            statement.setLong(1, documentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Issue issue = new Issue();
                    issue.setId(result.getLong("id"));
                    issue.setDocumentId(result.getLong("document_id"));
                    issue.setSeverity(result.getString("severity"));
                    issue.setCategory(result.getString("category"));
                    issue.setRecommendation(result.getString("recommendation"));
                    issues.add(issue);
                }
            }
        }
        return issues;
    }
}
