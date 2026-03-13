package com.hashpass.controller;
import java.security.Principal;
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
import com.hashpass.service.EntryService;

@Controller
public class CredentialController {
    @Autowired
    private CredentialRepository credentialRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSession userSession;

    @Autowired
    private EntryService entryService;

    // Make current user available to all views (mustache fragments expect it)
    @ModelAttribute("user")
    public User populateUser() {
        User u = userSession.getUser();
        if (u == null) {
            return null;
        }
        // Reload user from DB to ensure lazy collections are available for views
        return userRepository.findById(u.getId()).orElse(null);
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/password-login")
    public String passwordLogin() {
        return "password-login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }

        principal.getName(); // Esto va con el Principal de SpringSecurity
        
        User currentUser = userSession.getUser(); // Esto va con el Principal de SpringSecurity
        model.addAttribute("user", currentUser);

        // 1. Obtener todas las contraseñas de este usuario
        List<Credential> userCredentials = credentialRepository.findByUserId(currentUser.getId());

        // Total de contraseñas guardadas
        model.addAttribute("totalCredentials", userCredentials.size());

        // Calcular contraseñas débiles (menos de 8 caracteres)
        // NUEVO: Ahora desciframos cada contraseña temporalmente para medir su longitud real
        long weakCredentials = userCredentials.stream()
                .filter(c -> {
                    try {
                        String realPassword = entryService.decrypt(c.getPasswordEncrypted());
                        return realPassword != null && realPassword.length() < 8;
                    } catch (Exception e) {
                        return false; // Si hay error al descifrar, la ignoramos para esta cuenta
                    }
                })
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
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        
        // 1. Obtenemos la lista cifrada
        List<Credential> encryptedList = entryService.listCurrentUser();
        
        // 2. NUEVO: Creamos una copia de la lista donde reemplazamos el texto cifrado por el texto real temporalmente
        // Esto solo afecta a lo que se envía al HTML, en la base de datos sigue cifrado
        encryptedList.forEach(cred -> {
            try {
                String plain = entryService.decrypt(cred.getPasswordEncrypted());
                cred.setPasswordEncrypted(plain); // Reutilizamos el campo solo para mostrárselo al HTML
            } catch (Exception e) {
                cred.setPasswordEncrypted("Error al descifrar");
            }
        });
        
        model.addAttribute("credentials", encryptedList);
        return "passwords";
    }

    @GetMapping("/add-password")
    public String addPassword(Model model) {
        return requireLogin(model, "add-password");
    }

    @PostMapping("/add-password")
    public String savePassword(
            @RequestParam String service,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String note
    ) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        Credential c = new Credential();
        c.setSiteName(service);
        c.setUsername(username);
        c.setSiteUrl(url);
        c.setNote(note);
        entryService.save(c, password);
        return "redirect:/passwords";
    }

    @GetMapping("/info-passwords")
    public String infoPasswords(Model model, @RequestParam(required = false) Long id) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }

        if (id != null) {
            Optional<Credential> credOpt = entryService.findById(id);
            if (credOpt.isPresent()) {
                Credential cred = credOpt.get();
                // ensure the credential belongs to current user
                User current = userSession.getUser();
                if (cred.getUser() != null && cred.getUser().getId().equals(current.getId())) {
                    model.addAttribute("credential", cred);
                    model.addAttribute("decryptedPassword", entryService.decrypt(cred.getPasswordEncrypted()));
                } else {
                    return "redirect:/passwords";
                }
            } else {
                return "redirect:/passwords";
            }
        }

        model.addAttribute("credentials", entryService.listCurrentUser());
        return "info-passwords";
    }

    @PostMapping("/delete-password")
    public String deletePassword(@RequestParam Long id) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        Optional<Credential> credOpt = entryService.findById(id);
        if (credOpt.isPresent()) {
            Credential cred = credOpt.get();
            User current = userSession.getUser();
            if (cred.getUser() != null && cred.getUser().getId().equals(current.getId())) {
                entryService.delete(id);
            }
        }
        return "redirect:/passwords";
    }

    @PostMapping("/save-password-edit")
    public String savePasswordEdit(
            @RequestParam Long id,
            @RequestParam String service,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String note
    ) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }

        Optional<Credential> existing = entryService.findById(id);
        if (existing.isEmpty()) {
            return "redirect:/passwords";
        }

        Credential c = existing.get();
        User current = userSession.getUser();
        if (c.getUser() == null || !c.getUser().getId().equals(current.getId())) {
            return "redirect:/passwords";
        }

        c.setSiteName(service);
        c.setUsername(username);
        c.setSiteUrl(url);
        c.setNote(note);

        entryService.save(c, password);
        return "redirect:/passwords";
    }

}