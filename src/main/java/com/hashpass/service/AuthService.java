package com.hashpass.service;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.repository.PlanRepository;
import com.hashpass.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PlanRepository planRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final EntryService entryService;

    public AuthService(UserRepository userRepository, PlanRepository planRepository, UserService userService, PasswordEncoder passwordEncoder, EntryService entryService) {
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.entryService = entryService;
    }

    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public User registerUser(String name, String email, String password) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio.");
        }
        if (isEmailRegistered(normalizedEmail)) {
            throw new IllegalStateException("El correo ya está registrado.");
        }

        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(normalizedEmail);
        // Guardamos un hash de la contraseña usando BCrypt
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setEncryptionKey(deriveEncryptionKey(password));
        
        // Obtener el plan gratuito y asignarlo al nuevo usuario
        Optional<Plan> freePlan = planRepository.findByName("Gratuito");
        if (freePlan.isPresent()) {
            newUser.setPlan(freePlan.get());
        }
        
        try {
            return userRepository.save(newUser);
        } catch (DataIntegrityViolationException e) {
            // Evita error 500 si dos peticiones registran el mismo correo al mismo tiempo.
            throw new IllegalStateException("El correo ya está registrado.", e);
        }
    }

    public boolean login(String email, String password) {
        // Este método ya no se usa, ya que Spring Security maneja el login
        // Pero se mantiene por compatibilidad
        return false;
    }

    public void logout() {
        userService.logout();
        // Spring Security maneja el SecurityContext automáticamente
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public String changeEmail(User user, String currentPassword, String newEmail) {
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "Contraseña incorrecta.";
        }
        if (isEmailRegistered(newEmail)) {
            return "El correo electrónico ya está registrado.";
        }
        user.setEmail(newEmail);
        userRepository.save(user);
        return null; // Éxito
    }

    public String changeMasterPassword(User user, String currentPassword, String newPassword, String confirmPassword) {
        // 1. Validar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "La contraseña actual es incorrecta.";
        }
        
        // 2. Validar que la nueva contraseña no esté vacía
        if (newPassword == null || newPassword.isBlank()) {
            return "La nueva contraseña no puede estar vacía.";
        }
        
        // 3. Validar que las contraseñas coincidan
        if (!newPassword.equals(confirmPassword)) {
            return "Las contraseñas no coinciden.";
        }
        
        // 4. Validar que la nueva contraseña sea diferente
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return "La nueva contraseña debe ser distinta a la actual.";
        }

        // 5. Re-cifrar todas las credenciales del usuario
        String reEncryptError = entryService.reEncryptAllCredentials(user.getId(), currentPassword, newPassword);
        if (reEncryptError != null) {
            return reEncryptError;
        }

        // 6. Guardar la contraseña anterior (hasheada)
        user.setPreviousPasswordHash(user.getPasswordHash());
        user.setPasswordChangeTime(LocalDateTime.now());

        // 7. Actualizar la contraseña actual
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setEncryptionKey(deriveEncryptionKey(newPassword));
        
        // 8. Guardar el usuario
        userRepository.save(user);
        
        return null; // Éxito
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
        u.setFailedAttempts(0); // Si entra, reseteamos los fallos a 0
        userRepository.save(u);
    });
}
    public void loginSuccess(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            u.setLastLogin(java.time.LocalDateTime.now());
            u.setFailedAttempts(0); // Reiniciamos a 0 porque ha entrado
            userRepository.save(u);
        });
    }

    public void loginFailed(String email) {
        userRepository.findByEmail(email).ifPresent(u -> {
            int actual = (u.getFailedAttempts() != null) ? u.getFailedAttempts() : 0;
            u.setFailedAttempts(actual + 1); // Sumamos uno al fallo
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