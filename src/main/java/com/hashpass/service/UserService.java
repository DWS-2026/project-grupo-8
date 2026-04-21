package com.hashpass.service;


import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

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
    
}
