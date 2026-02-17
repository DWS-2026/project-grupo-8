package com.hashpass.service;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.hashpass.model.*;
import com.hashpass.repository.*;

import java.util.ArrayList;

@Component
public class DatabaseInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // 1. Crear Usuario Administrador (Requisito punto 870)
        User admin = new User();
        admin.setName("Admin");
        admin.setEmail("admin@hashpass.es");
        admin.setEncodedPassword(passwordEncoder.encode("adminpass"));
        // Aquí podrías añadir un rol de ADMIN si lo configuras en User
        userRepository.save(admin);

        // 2. Crear Usuarios de ejemplo (Requisito punto 977)
        User user1 = new User();
        user1.setName("Pepe");
        user1.setEmail("pepe@gmail.com");
        user1.setEncodedPassword(passwordEncoder.encode("pass123"));
        userRepository.save(user1);

        // 3. Crear Credenciales de ejemplo (Tu antiguo Post)
        Credential c1 = new Credential();
        c1.setUrl("netflix.com");
        c1.setUsername("pepe_netflix");
        c1.setPassword("secret123");
        c1.setOwner(user1);
        credentialRepository.save(c1);
    }
}