package com.hashpass.service;

import java.util.Optional;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.hashpass.model.User;
import com.hashpass.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSession userSession;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository, UserSession userSession, AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.userSession = userSession;
        this.authenticationManager = authenticationManager;
    }

    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public void registerUser(String name, String email, String password) {
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        // Guardamos un hash de la contraseña, no la contraseña en texto plano
        newUser.setPasswordHash(hashPassword(password));
        userRepository.save(newUser);
    }

    public boolean login(String email, String password) {
        try {
            // Derivar la clave de encriptación antes de autenticar
            String secretKey = deriveKey(password);
            userSession.setEncryptionKey(secretKey);

            // Autenticar con Spring Security
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Obtener el usuario y setear en userSession
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                userSession.setUser(userOpt.get());
            }

            return true;
        } catch (AuthenticationException e) {
            return false;
        }
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