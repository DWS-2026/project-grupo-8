package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
            RedirectAttributes redirectAttributes) {

        if (authService.login(email, password)) {
            return "redirect:/dashboard";
        }

        redirectAttributes.addAttribute("email", email);

        redirectAttributes.addFlashAttribute("error", "Contraseña incorrecta. Inténtelo de nuevo.");

        return "redirect:/password-login";
    }

    @PostMapping("/logout")
    public String logout() {
        authService.logout();
        return "redirect:/";
    }
}