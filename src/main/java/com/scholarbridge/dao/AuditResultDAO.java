package com.scholarbridge.dao;

import com.scholarbridge.config.DatabaseConfig;
import com.scholarbridge.model.AuditResult;

import java.sql.*;
import java.util.Optional;

public class AuditResultDAO {
    public void upsert(AuditResult audit) throws SQLException {
        String sql = "INSERT INTO audit_results "
                + "(document_id,quality_score,total_issues,critical_count,medium_count,low_count,score_breakdown) "
                + "VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE quality_score=VALUES(quality_score),"
                + "total_issues=VALUES(total_issues),critical_count=VALUES(critical_count),"
                + "medium_count=VALUES(medium_count),low_count=VALUES(low_count),score_breakdown=VALUES(score_breakdown)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, audit.getDocumentId());
            statement.setInt(2, audit.getQualityScore());
            statement.setInt(3, audit.getTotalIssues());
            statement.setInt(4, audit.getCriticalCount());
            statement.setInt(5, audit.getMediumCount());
            statement.setInt(6, audit.getLowCount());
            statement.setString(7, audit.getScoreBreakdown());
            statement.executeUpdate();
        }
    }

    public Optional<AuditResult> findByDocumentId(long documentId) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM audit_results WHERE document_id=?")) {
            statement.setLong(1, documentId);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                AuditResult audit = new AuditResult();
                audit.setId(result.getLong("id"));
                audit.setDocumentId(result.getLong("document_id"));
                audit.setQualityScore(result.getInt("quality_score"));
                audit.setTotalIssues(result.getInt("total_issues"));
                audit.setCriticalCount(result.getInt("critical_count"));
                audit.setMediumCount(result.getInt("medium_count"));
                audit.setLowCount(result.getInt("low_count"));
                audit.setScoreBreakdown(result.getString("score_breakdown"));
                return Optional.of(audit);
            }
        }
    }
}
