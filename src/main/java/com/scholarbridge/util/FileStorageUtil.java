package com.scholarbridge.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public final class FileStorageUtil {
    private final Path root;

    public FileStorageUtil() {
        String configured = System.getenv("SCHOLARBRIDGE_UPLOAD_DIR");
        root = configured == null || configured.isBlank()
                ? Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize()
                : Paths.get(configured).toAbsolutePath().normalize();
    }

    public StoredFile store(String originalFilename, String contentType, long size, InputStream input)
            throws IOException {
        Files.createDirectories(root);
        String extension = extensionOf(originalFilename);
        if (!".pdf".equalsIgnoreCase(extension)) throw new IOException("Only PDF files are accepted");
        String serverName = UUID.randomUUID() + extension;
        Path destination = root.resolve(serverName).normalize();
        if (!destination.startsWith(root)) throw new IOException("Unsafe file path");
        Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        return new StoredFile(serverName, destination, size, contentType);
    }

    public void delete(String serverFilename) throws IOException {
        if (serverFilename == null || serverFilename.contains("..") || serverFilename.contains("/") || serverFilename.contains("\\")) {
            throw new IOException("Unsafe file name");
        }
        Files.deleteIfExists(root.resolve(serverFilename).normalize());
    }

    public Path resolve(String serverFilename) throws IOException {
        if (serverFilename == null || serverFilename.contains("..") || serverFilename.contains("/") || serverFilename.contains("\\")) {
            throw new IOException("Unsafe file name");
        }
        Path path = root.resolve(serverFilename).normalize();
        if (!path.startsWith(root) || !Files.isRegularFile(path)) throw new IOException("File not found");
        return path;
    }

    private String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot);
    }

    public record StoredFile(String serverName, Path absolutePath, long size, String contentType) {}
}
