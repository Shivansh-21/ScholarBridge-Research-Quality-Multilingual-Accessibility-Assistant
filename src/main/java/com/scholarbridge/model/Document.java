package com.scholarbridge.model;

import java.time.LocalDateTime;

public class Document {
    private long id;
    private String originalFilename;
    private String serverFilename;
    private String filePath;
    private long fileSize;
    private String mimeType;
    private LocalDateTime uploadDate;
    private String title;
    private String authors;
    private String abstractText;
    private String doi;
    private String keywords;
    private String referencesText;
    private String extractedText;
    private String analysisStatus;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getServerFilename() { return serverFilename; }
    public void setServerFilename(String serverFilename) { this.serverFilename = serverFilename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }
    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }
    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getReferencesText() { return referencesText; }
    public void setReferencesText(String referencesText) { this.referencesText = referencesText; }
    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }
    public String getAnalysisStatus() { return analysisStatus; }
    public void setAnalysisStatus(String analysisStatus) { this.analysisStatus = analysisStatus; }
}
