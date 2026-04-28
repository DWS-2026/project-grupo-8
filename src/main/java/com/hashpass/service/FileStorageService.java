package com.hashpass.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for handling file storage operations on disk.
 * Files are stored with a unique name to avoid conflicts while preserving original filename.
 * 
 * Security features:
 * - Path traversal protection via normalization and validation
 * - Sanitized filenames to prevent directory traversal
 * - Strict file size limits
 * - All file operations validated to stay within storage directory
 */
@Service
public class FileStorageService {

    // Maximum file size: 10 MB (also configured in application.properties)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    // Characters that could be used for path traversal or cause issues
    private static final String FORBIDDEN_CHARS = "\\/:*?\"<>|";

    @Value("${file.storage.path:data/files}")
    private String storagePath;
    
    private Path normalizedStoragePath;
    
    /**
     * Initialize and normalize storage path for consistent security checks.
     * Called during bean initialization.
     */
    public void initStoragePath() throws IOException {
        Path path = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(path);
        this.normalizedStoragePath = path;
    }

    /**
     * Stores a file on disk with a unique name and returns the unique filename.
     * Original filename is preserved in the returned metadata.
     *
     * @param file MultipartFile to store
     * @return Unique filename with original filename preserved
     * @throws IOException if file storage fails
     * @throws IllegalArgumentException if file is invalid or too large
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Validate file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File exceeds maximum size of 10MB");
        }

        // Initialize storage path if needed
        if (normalizedStoragePath == null) {
            initStoragePath();
        }

        // Extract and sanitize original filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename is required");
        }

        // Remove path separators and dangerous characters
        String sanitizedFilename = sanitizeFilename(originalFilename);
        if (sanitizedFilename.isBlank()) {
            throw new IllegalArgumentException("Filename contains only invalid characters");
        }

        // Generate unique filename with sanitized name
        String uniqueFilename = generateUniqueFilename(sanitizedFilename);

        // Resolve and normalize the file path
        Path filePath = normalizedStoragePath.resolve(uniqueFilename).normalize();

        // Security check: ensure resolved path is within storage directory
        if (!filePath.startsWith(normalizedStoragePath)) {
            throw new IOException("Invalid file path - path traversal attempt detected");
        }

        // Save file to disk
        Files.write(filePath, file.getBytes());

        return uniqueFilename;
    }

    /**
     * Retrieves a file from disk as byte array.
     *
     * @param uniqueFilename Unique filename to retrieve
     * @return File bytes
     * @throws IOException if file not found or read fails
     */
    public byte[] retrieveFile(String uniqueFilename) throws IOException {
        if (uniqueFilename == null || uniqueFilename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Initialize storage path if needed
        if (normalizedStoragePath == null) {
            initStoragePath();
        }

        // Resolve and normalize the file path
        Path filePath = normalizedStoragePath.resolve(uniqueFilename).normalize();

        // Security check: ensure file is within storage directory and exists
        if (!filePath.startsWith(normalizedStoragePath)) {
            throw new IOException("Invalid file path - access denied");
        }

        if (!Files.exists(filePath)) {
            throw new IOException("File not found: " + uniqueFilename);
        }

        // Additional check: ensure it's a regular file, not a directory
        if (!Files.isRegularFile(filePath)) {
            throw new IOException("Path is not a regular file: " + uniqueFilename);
        }

        return Files.readAllBytes(filePath);
    }

    /**
     * Deletes a file from disk.
     *
     * @param uniqueFilename Unique filename to delete
     * @throws IOException if deletion fails
     */
    public void deleteFile(String uniqueFilename) throws IOException {
        if (uniqueFilename == null || uniqueFilename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }

        // Initialize storage path if needed
        if (normalizedStoragePath == null) {
            initStoragePath();
        }

        // Resolve and normalize the file path
        Path filePath = normalizedStoragePath.resolve(uniqueFilename).normalize();

        // Security check: ensure file is within storage directory
        if (!filePath.startsWith(normalizedStoragePath)) {
            throw new IOException("Invalid file path - access denied");
        }

        // Only delete if it's a regular file (not directory)
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            Files.delete(filePath);
        }
    }

    /**
     * Gets the original filename from a unique filename.
     *
     * @param uniqueFilename Unique filename
     * @return Original filename
     */
    public String getOriginalFilename(String uniqueFilename) {
        if (uniqueFilename == null || uniqueFilename.isBlank()) {
            return null;
        }

        // Format: timestamp_originalname
        // Extract everything after the first underscore
        int separatorIndex = uniqueFilename.indexOf('_');
        if (separatorIndex > 0 && separatorIndex < uniqueFilename.length() - 1) {
            return uniqueFilename.substring(separatorIndex + 1);
        }

        return uniqueFilename;
    }

    /**
     * Checks if a file exists on disk.
     *
     * @param uniqueFilename Unique filename
     * @return true if file exists, false otherwise
     */
    public boolean fileExists(String uniqueFilename) {
        if (uniqueFilename == null || uniqueFilename.isBlank()) {
            return false;
        }

        try {
            // Initialize storage path if needed
            if (normalizedStoragePath == null) {
                initStoragePath();
            }

            // Resolve and normalize the file path
            Path filePath = normalizedStoragePath.resolve(uniqueFilename).normalize();

            // Security check: ensure file is within storage directory
            if (!filePath.startsWith(normalizedStoragePath)) {
                return false;
            }

            return Files.exists(filePath) && Files.isRegularFile(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sanitizes filename to prevent path traversal and other attacks.
     * Removes or replaces dangerous characters.
     *
     * @param filename Original filename
     * @return Sanitized filename
     */
    private String sanitizeFilename(String filename) {
        // Remove any path separators and parent directory references
        String sanitized = filename
            .replaceAll("\\\\", "") // Remove backslashes
            .replaceAll("/", "")    // Remove forward slashes
            .replaceAll("\\.\\.", "") // Remove double dots
            .replaceAll("^\\.", ""); // Remove leading dots

        // Remove forbidden characters
        for (char c : FORBIDDEN_CHARS.toCharArray()) {
            sanitized = sanitized.replace(c, '_');
        }

        return sanitized;
    }

    /**
     * Generates a unique filename to avoid collisions.
     * Format: timestamp_originalfilename
     *
     * @param originalFilename Original filename
     * @return Unique filename
     */
    private String generateUniqueFilename(String originalFilename) {
        long timestamp = Instant.now().toEpochMilli();
        return timestamp + "_" + originalFilename;
    }
}
