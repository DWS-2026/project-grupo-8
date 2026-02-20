package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/password-login")
    public String passwordLogin() {
        return "password-login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/passwords")
    public String passwords() {
        return "passwords";
    }

    @GetMapping("/add-password")
    public String addPassword() {
        return "add-password";
    }

    @GetMapping("/info-passwords")
    public String infoPasswords() {
        return "info-passwords";
    }

    @GetMapping("/plan")
    public String plan() {
        return "plan";
    }

    @GetMapping("/payment")
    public String payment() {
        return "payment";
    }

    @GetMapping("/user")
    public String user() {
        return "user";
    }

    @GetMapping("/config-user")
    public String configUser() {
        return "config_user";
    }

    @GetMapping("/security-user")
    public String securityUser() {
        return "security_user";
    }

    @GetMapping("/reviews")
    public String reviews() {
        return "reviews";
    }

    @GetMapping("/admin")
    public String admin() {
        return "admin";
    }

    @GetMapping("/admin-user-detail")
    public String adminUserDetail() {
        return "admin_user_detail";
    }
}