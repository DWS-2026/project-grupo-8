package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hashpass.service.AuthService;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        if (authService.isEmailRegistered(email)) {
            model.addAttribute("error", "El correo ya está registrado.");
            return "register";
        }

        authService.registerUser(name, email, password);
        return "redirect:/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email,
            @RequestParam String password,
            Model model) {

        if (authService.login(email, password)) {
            return "redirect:/dashboard";
        }

        model.addAttribute("error", "Correo o contraseña incorrectos.");
        return "login";
    }

    @GetMapping("/logout")
    public String logout() {
        authService.logout();
        return "redirect:/";
    }
}