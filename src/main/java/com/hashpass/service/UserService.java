package com.hashpass.service;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.repository.UserRepository;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

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
    
}
