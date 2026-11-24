package com.scholarbridge.dao;

import com.scholarbridge.config.DatabaseConfig;
import com.scholarbridge.model.Translation;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TranslationDAO {
    public void upsert(Translation translation) throws SQLException {
        String sql = "INSERT INTO translations (document_id,language,content_type,content,ai_generated) "
                + "VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE content=VALUES(content),"
                + "ai_generated=VALUES(ai_generated)";
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, translation.getDocumentId());
            statement.setString(2, translation.getLanguage());
            statement.setString(3, translation.getContentType());
            statement.setString(4, translation.getContent());
            statement.setBoolean(5, translation.isAiGenerated());
            statement.executeUpdate();
        }
    }

    public List<Translation> findByDocumentId(long documentId) throws SQLException {
        List<Translation> values = new ArrayList<>();
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT * FROM translations WHERE document_id=? ORDER BY created_at DESC")) {
            statement.setLong(1, documentId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Translation value = new Translation();
                    value.setId(result.getLong("id"));
                    value.setDocumentId(result.getLong("document_id"));
                    value.setLanguage(result.getString("language"));
                    value.setContentType(result.getString("content_type"));
                    value.setContent(result.getString("content"));
                    value.setAiGenerated(result.getBoolean("ai_generated"));
                    values.add(value);
                }
            }
        }
        return values;
    }
}
