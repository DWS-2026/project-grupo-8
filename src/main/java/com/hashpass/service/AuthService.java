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

    public User registerUser(String name, String email, String password) {
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        // Guardamos un hash de la contraseña usando BCrypt
        newUser.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(newUser);
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
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "La contraseña actual es incorrecta.";
        }
        if (newPassword == null || newPassword.isBlank()) {
            return "La nueva contraseña no puede estar vacía.";
        }
        if (!newPassword.equals(confirmPassword)) {
            return "La confirmación de la nueva contraseña no coincide.";
        }
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            return "La nueva contraseña debe ser distinta a la actual.";
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return null; // Éxito
    }

    public String deleteAccount(User user, String currentPassword) {
        if (user == null) {
            return "No hay ninguna sesión activa.";
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            return "La contraseña actual es incorrecta.";
        }

        User persistedUser = userRepository.findById(user.getId()).orElse(null);
        if (persistedUser == null) {
            return "No se ha encontrado la cuenta que se quiere eliminar.";
        }

        userRepository.delete(persistedUser);
        return null;
    }
}