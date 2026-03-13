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

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}