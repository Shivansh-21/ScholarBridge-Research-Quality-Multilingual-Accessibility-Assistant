CREATE DATABASE IF NOT EXISTS scholarbridge CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE scholarbridge;

CREATE TABLE IF NOT EXISTS documents (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  original_filename VARCHAR(255) NOT NULL,
  server_filename VARCHAR(255) NOT NULL UNIQUE,
  file_path VARCHAR(1024) NOT NULL,
  file_size BIGINT UNSIGNED NOT NULL,
  mime_type VARCHAR(100) NOT NULL,
  upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  title VARCHAR(500) NULL,
  authors TEXT NULL,
  abstract_text LONGTEXT NULL,
  doi VARCHAR(255) NULL,
  keywords TEXT NULL,
  references_text LONGTEXT NULL,
  extracted_text LONGTEXT NULL,
  analysis_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
  INDEX idx_documents_upload_date (upload_date),
  INDEX idx_documents_status (analysis_status)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS audit_results (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT UNSIGNED NOT NULL,
  quality_score INT NOT NULL,
  total_issues INT NOT NULL,
  critical_count INT NOT NULL DEFAULT 0,
  medium_count INT NOT NULL DEFAULT 0,
  low_count INT NOT NULL DEFAULT 0,
  score_breakdown TEXT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_audit_document (document_id),
  CONSTRAINT fk_audit_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  INDEX idx_audit_score (quality_score)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS issues (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT UNSIGNED NOT NULL,
  severity VARCHAR(20) NOT NULL,
  category VARCHAR(100) NOT NULL,
  recommendation TEXT NOT NULL,
  detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_issue_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
  INDEX idx_issues_document (document_id),
  INDEX idx_issues_severity (severity)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS translations (
  id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
  document_id BIGINT UNSIGNED NOT NULL,
  language VARCHAR(20) NOT NULL,
  content_type VARCHAR(30) NOT NULL,
  content TEXT NOT NULL,
  ai_generated BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uq_translation (document_id, language, content_type),
  CONSTRAINT fk_translation_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE
) ENGINE=InnoDB;
