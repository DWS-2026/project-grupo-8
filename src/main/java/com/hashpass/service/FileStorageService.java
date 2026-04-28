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
 */
@Service
public class FileStorageService {

    @Value("${file.storage.path:data/files}")
    private String storagePath;

    /**
     * Stores a file on disk with a unique name and returns the unique filename.
     * Original filename is preserved in the returned metadata.
     *
     * @param file MultipartFile to store
     * @return Unique filename with original filename preserved
     * @throws IOException if file storage fails
     */
    public String storeFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Create storage directory if it doesn't exist
        Path storageDirPath = Paths.get(storagePath);
        Files.createDirectories(storageDirPath);

        // Extract original filename
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("Original filename is required");
        }

        // Generate unique filename: timestamp_originalname
        String uniqueFilename = generateUniqueFilename(originalFilename);

        // Save file to disk
        Path filePath = storageDirPath.resolve(uniqueFilename);
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

        Path filePath = Paths.get(storagePath).resolve(uniqueFilename);

        // Security check: ensure file exists and is within storage directory
        if (!Files.exists(filePath) || !filePath.normalize().startsWith(Paths.get(storagePath).normalize())) {
            throw new IOException("File not found or invalid path: " + uniqueFilename);
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

        Path filePath = Paths.get(storagePath).resolve(uniqueFilename);

        // Security check: ensure file is within storage directory
        if (!filePath.normalize().startsWith(Paths.get(storagePath).normalize())) {
            throw new IOException("Invalid file path: " + uniqueFilename);
        }

        Files.deleteIfExists(filePath);
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
        // Extract everything after the first underscore (and the timestamp+underscore prefix)
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

        Path filePath = Paths.get(storagePath).resolve(uniqueFilename);
        return Files.exists(filePath);
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
