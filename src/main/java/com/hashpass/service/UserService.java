package com.hashpass.service;


import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.repository.PlanRepository;
import com.hashpass.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public Optional<User> getLoggedUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(auth.getName());
    }

    public User setUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("El usuario no puede ser nulo.");
        }
        return userRepository.save(user);
    }

    public Optional<User> updateLoggedUserPlan(Plan targetPlan) {
        if (targetPlan == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = getLoggedUser();
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        user.setPlan(targetPlan);
        return Optional.of(setUser(user));
    }

    public void logout() {
        SecurityContextHolder.clearContext();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findWithPlanById(Long id) {
        return userRepository.findWithPlanById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public Optional<Plan> findPlanById(Long id) {
        return planRepository.findById(id);
    }

    /**
     * Uploads and stores a document for a user.
     * If user already has a document, the old one is deleted first.
     *
     * @param userId User ID
     * @param file MultipartFile to upload
     * @return Updated User with new document
     * @throws IOException if file storage fails
     * @throws IllegalArgumentException if user not found
     */
    public User uploadUserDocument(Long userId, MultipartFile file) throws IOException {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }

        User user = userOpt.get();

        // Delete old document if exists
        if (user.getDocumentFileName() != null && !user.getDocumentFileName().isBlank()) {
            try {
                fileStorageService.deleteFile(user.getDocumentFileName());
            } catch (IOException e) {
                // Log but don't fail - old file may be missing
                System.err.println("Warning: Could not delete old document: " + e.getMessage());
            }
        }

        // Store new file and update user
        String uniqueFilename = fileStorageService.storeFile(file);
        user.setDocumentFileName(uniqueFilename);
        return setUser(user);
    }

    /**
     * Retrieves a user's document as bytes.
     *
     * @param userId User ID
     * @return File bytes
     * @throws IOException if file not found or read fails
     * @throws IllegalArgumentException if user not found or has no document
     */
    public byte[] getUserDocument(Long userId) throws IOException {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }

        User user = userOpt.get();
        if (user.getDocumentFileName() == null || user.getDocumentFileName().isBlank()) {
            throw new IllegalArgumentException("User has no document");
        }

        return fileStorageService.retrieveFile(user.getDocumentFileName());
    }

    /**
     * Gets the original filename of a user's document.
     *
     * @param userId User ID
     * @return Original filename or null if no document
     * @throws IllegalArgumentException if user not found
     */
    public String getUserDocumentOriginalFilename(Long userId) {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }

        User user = userOpt.get();
        if (user.getDocumentFileName() == null || user.getDocumentFileName().isBlank()) {
            return null;
        }

        return fileStorageService.getOriginalFilename(user.getDocumentFileName());
    }

    /**
     * Checks if a user has a document.
     *
     * @param userId User ID
     * @return true if user has document, false otherwise
     */
    public boolean userHasDocument(Long userId) {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            return false;
        }

        User user = userOpt.get();
        return user.getDocumentFileName() != null && !user.getDocumentFileName().isBlank();
    }

    /**
     * Deletes a user's document.
     *
     * @param userId User ID
     * @throws IllegalArgumentException if user not found
     */
    public void deleteUserDocument(Long userId) throws IOException {
        Optional<User> userOpt = findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + userId);
        }

        User user = userOpt.get();
        if (user.getDocumentFileName() != null && !user.getDocumentFileName().isBlank()) {
            fileStorageService.deleteFile(user.getDocumentFileName());
            user.setDocumentFileName(null);
            setUser(user);
        }
    }
    
}
