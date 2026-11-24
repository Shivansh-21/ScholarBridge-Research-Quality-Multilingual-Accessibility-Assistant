# ScholarBridge

ScholarBridge is a Java 17 web application for PDF research-paper upload, rule-based metadata extraction, publication-quality auditing, and multilingual accessibility.

## Technology

- Java 17
- Jakarta Servlet API 6.0
- Apache Maven
- Apache Tomcat 10.1
- Apache PDFBox
- MySQL 8 and MySQL Connector/J
- JDBC with PreparedStatement
- JUnit 5
- HTML5, CSS3, and vanilla JavaScript

## Features

- PDF validation, server-generated names, safe filesystem storage, and text extraction
- Metadata detection for title, authors, abstract, DOI, keywords, and references
- Transparent quality audit with an initial 100-point score
- Hindi, Punjabi, and simplified-English AI output through an OpenAI-compatible HTTP API
- MySQL-backed document history, audits, issues, and generated translations
- Responsive dashboard, upload, history, analysis, and accessibility pages

## Quality scoring

The audit starts at 100. It applies these deductions:

| Issue | Deduction |
|---|---:|
| Missing title | 15 |
| Missing authors | 10 |
| Missing abstract | 15 |
| Missing keywords | 10 |
| Missing DOI | 10 |
| Malformed DOI format | 10 |
| Incomplete references | 10 |
| Missing four or more basic sections | 20 |

The score is clamped to zero. Severity classification distinguishes missing title/abstract/basic sections and malformed DOI as critical/high; missing DOI/keywords/references as medium. DOI format validation does not verify that a DOI is registered or exists.

## Prerequisites

- JDK 17
- Apache Maven
- MySQL 8
- Apache Tomcat 10.1

## Database setup

Create the database and tables with:

```bash
mysql -u root -p < src/main/resources/schema.sql
```

Configure MySQL through environment variables:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/scholarbridge?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER="root"
$env:DB_PASSWORD="<your-password>"
```

For production, keep the password in `DB_PASSWORD`. No database password is stored in source code.

## AI accessibility configuration

The AI integration is optional. Set an OpenAI-compatible endpoint and credentials:

```powershell
$env:AI_API_KEY="<api-key>"
$env:AI_ENDPOINT="https://provider.example/v1/chat/completions"
$env:AI_MODEL="gpt-4o-mini"
```

Without these variables, the application remains usable and reports that AI configuration is missing. API credentials are only read by Java server code and are never returned to frontend JavaScript.

## Upload storage

By default, PDFs are stored in `uploads/` under the server working directory. Change this with:

```powershell
$env:SCHOLARBRIDGE_UPLOAD_DIR="C:\ScholarBridge\uploads"
```

The configured directory must be writable and is outside the public web directory.

## Build and test

```powershell
mvn clean test
mvn clean package
```

The WAR is produced at `target/ScholarBridge.war`.

## Deploy to Tomcat 10.1

Set environment variables in the Tomcat process or service, then deploy:

```powershell
Copy-Item target\ScholarBridge.war C:\apache-tomcat-10.1.x\webapps\
& C:\apache-tomcat-10.1.x\bin\startup.bat
```

Open:

- `http://localhost:8080/ScholarBridge/dashboard.html`
- `http://localhost:8080/ScholarBridge/upload.html`

## API endpoints

- `POST /api/upload`
- `GET /api/dashboard`
- `GET /api/documents`
- `GET /api/documents/{id}`
- `DELETE /api/documents/{id}`
- `GET /api/audit/{id}`
- `GET /api/translations/{id}`
- `POST /api/translations/{id}` with `{"language":"Hindi"}`, `{"language":"Punjabi"}`, or `{"language":"simple_english"}`

## Main project structure

```text
src/main/java/com/scholarbridge/
  config/  DatabaseConfig.java
  model/   Document, AuditResult, Issue, Translation
  dao/     JDBC data access classes
  service/ PDF, DOI, audit, translation, and document services
  servlet/ JSON HTTP endpoints
  util/    JSON and safe file-storage utilities
src/main/resources/schema.sql
src/main/webapp/ HTML, CSS, JavaScript
src/test/java/ JUnit 5 tests
```
