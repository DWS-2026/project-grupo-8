package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hashpass.model.User;
import com.hashpass.service.AuthService;
import com.hashpass.service.UserSession;

@Controller
public class UserController {

    private final AuthService authService;
    private final UserSession userSession;

    @ModelAttribute("user")
    public User populateUser() {
        return userSession.getUser();
    }

    public UserController(AuthService authService, UserSession userSession) {
        this.authService = authService;
        this.userSession = userSession;
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

    @GetMapping("/user")
    public String user(Model model) {
        return requireLogin(model, "user");
    }

    @GetMapping("/config-user")
    public String configUser(Model model) {
        return requireLogin(model, "config_user");
    }

    @GetMapping("/security-user")
    public String securityUser(Model model) {
        return requireLogin(model, "security_user");
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        return requireLogin(model, "admin");
    }

    @GetMapping("/admin-user-detail")
    public String adminUserDetail(Model model) {
        return requireLogin(model, "admin_user_detail");
    }

    // helper to require login and automatically supply user via @ModelAttribute
    private String requireLogin(Model model, String view) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        // user already added by populateUser()
        return view;
    }
}