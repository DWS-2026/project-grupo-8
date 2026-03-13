package com.hashpass.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hashpass.model.User;
import com.hashpass.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSession userSession;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, UserSession userSession, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userSession = userSession;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public void registerUser(String name, String email, String password) {
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        // Guardamos un hash de la contraseña usando BCrypt
        newUser.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(newUser);
    }

    public boolean login(String email, String password) {
        // Este método ya no se usa, ya que Spring Security maneja el login
        // Pero se mantiene por compatibilidad
        return false;
    }

    public void logout() {
        userSession.logout();
        // Spring Security maneja el SecurityContext automáticamente
    }

    // ------------------------------------------------------------------
    // Este codigo hace que la llave sea de 32 caracteres exactamente para poder
    // usarla en el cifrado.
    // ------------------------------------------------------------------
    private String deriveKey(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            // Aquí le decimos: "Pase lo que pase, dame solo los primeros 32 caracteres"
            return hexString.toString().substring(0, 32);
        } catch (Exception e) {
            throw new RuntimeException("Error generando la llave maestra", e);
        }
    }

    // ------------------------------------------------------------------
    // Helper para guardar la contraseña maestra como un hash
    // ------------------------------------------------------------------
    private String hashPassword(String password) {
        try {
            // Usamos SHA-256 para generar un hash de la contraseña, que es lo que se guarda en la base de datos
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1)
                    hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString(); // Devolvemos el hash completo, no lo truncamos porque no es una llave de cifrado, es solo para verificar la contraseña
        } catch (Exception e) {
            throw new RuntimeException("Error al hashear la contraseña", e);
        }
    }
}