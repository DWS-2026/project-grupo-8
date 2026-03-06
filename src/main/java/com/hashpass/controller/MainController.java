package com.hashpass.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.hashpass.model.Credential;
import com.hashpass.model.User;
import com.hashpass.repository.CredentialRepository;
import com.hashpass.repository.UserRepository;
import com.hashpass.service.UserSession;

@Controller
public class MainController {

    @Autowired
    private CredentialRepository credentialRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSession userSession;

    // Make current user available to all views (mustache fragments expect it)
    @ModelAttribute("user")
    public User populateUser() {
        return userSession.getUser();
    }

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
        
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }

        User currentUser = userSession.getUser();
        model.addAttribute("user", currentUser);

        // 1. Obtener todas las contraseñas de este usuario
        List<Credential> userCredentials = credentialRepository.findByUserId(currentUser.getId());

        // Total de contraseñas guardadas
        model.addAttribute("totalCredentials", userCredentials.size());

        // Calcular contraseñas débiles (menos de 8 caracteres)
        long weakCredentials = userCredentials.stream()
                .filter(c -> c.getPasswordEncrypted().length() < 8)
                .count();
        model.addAttribute("weakCredentials", weakCredentials);

        // Calcular accesos/modificaciones en los últimos 30 días
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        long monthlyAccesses = userCredentials.stream()
                .filter(c -> c.getUpdatedAt().isAfter(thirtyDaysAgo))
                .count();
        model.addAttribute("monthlyAccesses", monthlyAccesses);

        model.addAttribute("recentActivity", userCredentials);

        return "dashboard";
    }

    // helper to require login and automatically supply user via @ModelAttribute
    private String requireLogin(Model model, String view) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        // user already added by populateUser()
        return view;
    }

    @GetMapping("/passwords")
    public String passwords(Model model) {
        return requireLogin(model, "passwords");
    }

    @GetMapping("/add-password")
    public String addPassword(Model model) {
        return requireLogin(model, "add-password");
    }

    @GetMapping("/info-passwords")
    public String infoPasswords(Model model) {
        return requireLogin(model, "info-passwords");
    }

    @GetMapping("/plan")
    public String plan(Model model) {
        return requireLogin(model, "plan");
    }

    @GetMapping("/payment")
    public String payment(Model model) {
        return requireLogin(model, "payment");
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

    @GetMapping("/reviews")
    public String reviews(Model model) {
        return requireLogin(model, "reviews");
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        return requireLogin(model, "admin");
    }

    @GetMapping("/admin-user-detail")
    public String adminUserDetail(Model model) {
        return requireLogin(model, "admin_user_detail");
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