package com.hashpass.controller.rest;

import org.springframework.web.bind.annotation.RestController;

import com.hashpass.model.User;
import com.hashpass.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class UserRestController {
    
    private UserRepository userRepository;

    @GetMapping("/api/v1/users/{id}")
    public User getUserById(@RequestParam Long id) {
        return userRepository.findById(id).orElse(null);
    }

}
