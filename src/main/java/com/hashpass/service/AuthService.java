package com.hashpass.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.security.HtmlSanitizer;
import com.hashpass.repository.PlanRepository;
import com.hashpass.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EntryService entryService;
    private final HtmlSanitizer htmlSanitizer;

    public AuthService(UserRepository userRepository, PlanRepository planRepository, UserService userService,
            PasswordEncoder passwordEncoder, EntryService entryService, HtmlSanitizer htmlSanitizer) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.entryService = entryService;
        this.htmlSanitizer = htmlSanitizer;
    }

    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(String name, String email, String password,String password2, Long selectedPlanId, boolean allowPaidPlan) {
        String normalizedEmail = htmlSanitizer.normalizeEmail(email);
        String normalizedName = htmlSanitizer.sanitizePlainText(name);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }
        if (normalizedName == null || normalizedName.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio.");
        }
        if (normalizedName.length() > 120) {
            throw new IllegalArgumentException("El nombre no puede superar los 120 caracteres.");
        }
        if (isEmailRegistered(normalizedEmail)) {
            throw new IllegalStateException("El correo ya está registrado.");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria.");
        }
        if (!password.equals(password2)) {
            throw new IllegalArgumentException("Las contraseñas no coinciden.");
        }

        User newUser = new User();
        newUser.setName(normalizedName);
        newUser.setEmail(normalizedEmail);
        // Store a password hash using BCrypt
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setEncryptionKey(deriveEncryptionKey(password));
        
        Optional<Plan> selectedPlanOpt = selectedPlanId == null
                ? Optional.empty()
                : planRepository.findById(selectedPlanId);

        Plan selectedPlan = selectedPlanOpt.orElse(null);
        Plan freePlan = planRepository.findByName("Gratuito").orElse(null);
        Plan planToAssign = freePlan;
        if (selectedPlan != null) {
            BigDecimal price = selectedPlan.getPriceMonthly() == null ? BigDecimal.ZERO : selectedPlan.getPriceMonthly();
            boolean isPaidPlan = price.compareTo(BigDecimal.ZERO) > 0;
            if (!isPaidPlan || allowPaidPlan) {
                planToAssign = selectedPlan;
            }
        }

        if (planToAssign != null) {
            newUser.setPlan(planToAssign);
        }
        
        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            // Avoid a 500 error if two requests register the same email at the same time.
            throw new IllegalStateException("El correo ya está registrado.", e);
        }
    }

    public boolean login(String email, String password) {
        // This method is no longer used because Spring Security handles login
        // It is kept for compatibility
        return false;
    }

    public void logout() {
        userService.logout();
        // Spring Security handles the SecurityContext automatically
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public String changeEmail(User user, String currentPassword, String newEmail) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "Contraseña incorrecta.";
        }
        String normalizedEmail = htmlSanitizer.normalizeEmail(newEmail);
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return "El correo electrónico es obligatorio.";
        }
        if (isEmailRegistered(normalizedEmail)) {
            return "El correo electrónico ya está registrado.";
        }
        user.setEmail(normalizedEmail);
        userRepository.save(user);
        return null; // Success
    }

    public String changeMasterPassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        // 1. Validate the current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "La contraseña actual es incorrecta.";
        }
        
        // 2. Validate that the new password is not empty
        if (newPassword == null || newPassword.isBlank()) {
            return "La nueva contraseña no puede estar vacía.";
        }
        
        // 3. Validate that passwords match
        if (!newPassword.equals(confirmPassword)) {
            return "Las contraseñas no coinciden.";
        }
        
        // 4. Validate that the new password is different
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return "La nueva contraseña debe ser distinta a la actual.";
        }

        // 5. Re-encrypt all user credentials
        String reEncryptError = entryService.reEncryptAllCredentials(user.getId(), currentPassword, newPassword);
        if (reEncryptError != null) {
            return reEncryptError;
        }

        // 6. Store previous password hash
        user.setPreviousPasswordHash(user.getPasswordHash());
        user.setPasswordChangeTime(LocalDateTime.now());

        // 7. Update current password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEncryptionKey(deriveEncryptionKey(newPassword));
        
        // 8. Save the user
        userRepository.save(user);
        
        return null; // Success
    }

    public String deleteAccount(User user, String currentPassword) {
        if (user == null) {
            return "No hay ninguna sesión activa.";
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "La contraseña es incorrecta.";
        }

        User persistedUser = userRepository.findById(user.getId()).orElse(null);
        if (persistedUser == null) {
            return "No se ha encontrado la cuenta que se quiere eliminar.";
        }

        userRepository.delete(persistedUser);
        return null;
    }
    public void actualizarUltimoLogin(String email) {
    userRepository.findByEmail(email).ifPresent(u -> {
        u.setLastLogin(java.time.LocalDateTime.now());
        u.setFailedAttempts(0); // On successful login, reset failed attempts to 0
        userRepository.save(u);
    });
}
    public void loginSuccess(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setLastLogin(java.time.LocalDateTime.now());
            u.setFailedAttempts(0); // Reset to 0 after successful login
            userRepository.save(u);
        });
    }

    public void loginFailed(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            int actual = (u.getFailedAttempts() != null) ? u.getFailedAttempts() : 0;
            u.setFailedAttempts(actual + 1); // Increment failed attempts
            userRepository.save(u);
        });
    }

    private String deriveEncryptionKey(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString().substring(0, 32);
        } catch (Exception e) {
            throw new RuntimeException("Error derivando la llave de cifrado", e);
        }
    }
}