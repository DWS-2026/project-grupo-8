package com.hashpass.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hashpass.model.User;
import com.hashpass.repository.UserRepository;
import com.hashpass.service.UserSession;

@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSession userSession;

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
    public String dashboard(Model model) {
        // 1. Comprobar si hay un usuario en sesión
        if (!userSession.isLogged()) {
            return "redirect:/login"; // Si no está logueado, patada a la pantalla de login
        }

        // 2. Si está logueado, pasamos el objeto User completo a la vista de Mustache
        model.addAttribute("user", userSession.getUser());
        
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

    // --- LÓGICA DE REGISTRO ---
    @PostMapping("/register")
    public String processRegister(@RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        // Comprobar si el correo ya existe
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "El correo ya está registrado.");
            return "register";
        }

        // Crear el nuevo usuario
        User newUser = new User();
        newUser.setName(name);
        newUser.setEmail(email);

        // Cambiar más adelante por un hash real
        newUser.setPasswordHash(password);

        // Guardar en MySQL
        userRepository.save(newUser);

        return "redirect:/login"; // Si va bien, lo mandamos a que haga login
    }

    // --- LÓGICA DE LOGIN ---
    @PostMapping("/login")
    public String processLogin(@RequestParam String email,
            @RequestParam String password,
            Model model) {

        // Buscar al usuario por correo
        Optional<User> userOpt = userRepository.findByEmail(email);

        // Si existe y la contraseña coincide
        if (userOpt.isPresent() && userOpt.get().getPasswordHash().equals(password)) {
            // Guardamos el usuario en la sesión
            userSession.setUser(userOpt.get());
            return "redirect:/dashboard"; // Lo llevamos a su panel
        } else {
            // Falla el login
            model.addAttribute("error", "Correo o contraseña incorrectos.");
            return "login";
        }
    }

    // --- LÓGICA DE LOGOUT ---
    @GetMapping("/logout")
    public String logout() {
        userSession.setUser(null);
        return "redirect:/";
    }

}