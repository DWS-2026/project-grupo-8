package com.hashpass.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hashpass.model.User;
import com.hashpass.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSession userSession;

    public AuthService(UserRepository userRepository, UserSession userSession) {
        this.userRepository = userRepository;
        this.userSession = userSession;
    }

    public boolean isEmailRegistered(String email) {
        return userRepository.existsByEmail(email);
    }

    public void registerUser(String name, String email, String password) {
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);
        newUser.setPasswordHash(password);
        userRepository.save(newUser);
    }

    public boolean login(String email, String password) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent() && userOpt.get().getPasswordHash().equals(password)) {
            userSession.setUser(userOpt.get());
            return true;
        }
        return false;
    }

    public void logout() {
        userSession.setUser(null);
    }
}