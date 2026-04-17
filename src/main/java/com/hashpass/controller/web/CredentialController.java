package com.hashpass.controller.web;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.hashpass.model.Credential;
import com.hashpass.model.User;
import com.hashpass.service.UserService;

import com.hashpass.service.EntryService;
import com.hashpass.service.ImageService;
import com.hashpass.service.ReviewService;

@Controller
public class CredentialController {
    @Autowired
    private UserService userService;

    @Autowired
    private EntryService entryService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private ReviewService reviewService;

    private static final DateTimeFormatter INDEX_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Make current user available to all views (mustache fragments expect it)
    @ModelAttribute("user")
    public User populateUser() {
        Optional<User> u = userService.getLoggedUser();
        if (!u.isPresent()) {
            return null;
        }
        // Reload user from DB to ensure lazy collections are available for views
        return userService.findById(u.get().getId()).orElse(null);
    }

    @ModelAttribute("profileImageUrl")
    public String populateProfileImageUrl() {
        return imageService.getProfileImageUrl(userService.getLoggedUser());
    }

    @ModelAttribute("isLogged")
    public boolean populateIsLogged() {
        return userService.getLoggedUser().isPresent();
    }

    @GetMapping("/")
    public String index(Model model) {
        List<Map<String, Object>> topReviews = reviewService.findTop3ByOrderByRatingDescCreatedAtDesc()
                .stream().map(review -> {
                    Map<String, Object> m = new HashMap<>();
                    User reviewUser = review.getUser();
                    String authorName = (reviewUser != null && reviewUser.getName() != null && !reviewUser.getName().isBlank())
                            ? reviewUser.getName()
                            : (reviewUser != null ? reviewUser.getEmail() : "Usuario de HashPass");
                    String avatarUrl = imageService.getProfileImageUrl(Optional.ofNullable(reviewUser));
                    if (avatarUrl == null) {
                        avatarUrl = "https://ui-avatars.com/api/?name="
                                + URLEncoder.encode(authorName, StandardCharsets.UTF_8)
                                + "&background=random";
                    }
                    List<Map<String, Object>> stars = new ArrayList<>();
                    int safeRating = review.getRating() == null ? 0 : Math.max(0, Math.min(5, review.getRating()));
                    for (int i = 1; i <= 5; i++) {
                        Map<String, Object> star = new HashMap<>();
                        star.put("filled", i <= safeRating);
                        stars.add(star);
                    }
                    m.put("comment", review.getComment());
                    m.put("authorName", authorName);
                    m.put("avatarUrl", avatarUrl);
                    m.put("stars", stars);
                    return m;
                }).toList();
        model.addAttribute("topReviews", topReviews);
        model.addAttribute("hasTopReviews", !topReviews.isEmpty());
        return "index";
    }

    @GetMapping("/password-login")
    public String passwordLogin(@RequestParam(required = false) String email,
                                @RequestParam(required = false) String redirectTo,
                                @RequestParam(required = false) String error,
                                Model model) {
        if (email == null || email.isBlank()) {
            return "redirect:/login";
        }
        if (userService.findByEmail(email).isEmpty()) {
            return "redirect:/login?error=1";
        }

        model.addAttribute("email", email);
        model.addAttribute("redirectTo", sanitizeRedirectTarget(redirectTo));

        if (error != null) {
            model.addAttribute("error", "Contraseña maestra incorrecta.");
        }
        return "password-login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }

        principal.getName(); // Keep Principal in use for Spring Security
        
        User currentUser = userService.getLoggedUser().get(); 
        model.addAttribute("user", currentUser);

        // 1. Get all credentials for this user
        List<Credential> userCredentials = entryService.listCurrentUser();

        // Total number of stored credentials
        model.addAttribute("totalCredentials", userCredentials.size());

        // Compute weak passwords (less than 8 characters)
        // NEW: Decrypt each password temporarily to measure real length
        long weakCredentials = userCredentials.stream()
                .filter(c -> {
                    try {
                        String realPassword = entryService.decrypt(c.getPasswordEncrypted(), userService.getLoggedUser());
                        return realPassword != null && realPassword.length() < 8;
                    } catch (Exception e) {
                        return false; // If decryption fails, ignore this item for this metric
                    }
                })
                .count();
        model.addAttribute("weakCredentials", weakCredentials);

        // Use login counter stored in User for the last 30 days
        Integer cnt = currentUser.getLoginCount();
        model.addAttribute("monthlyAccesses", cnt == null ? 0 : cnt);

        model.addAttribute("recentActivity", userCredentials);

        return "dashboard";
    }

    @GetMapping("/passwords")
    public String passwords(Model model, @RequestParam(required = false) String q) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }

        // 1. Get the encrypted list
        List<Credential> encryptedList = entryService.listCurrentUser();

        String query = q == null ? "" : q.trim().toLowerCase();
        if (!query.isBlank()) {
            encryptedList = encryptedList.stream()
                    .filter(cred -> {
                        String siteName = cred.getSiteName() == null ? "" : cred.getSiteName().toLowerCase();
                        String username = cred.getUsername() == null ? "" : cred.getUsername().toLowerCase();
                        String siteUrl = cred.getSiteUrl() == null ? "" : cred.getSiteUrl().toLowerCase();
                        return siteName.contains(query) || username.contains(query) || siteUrl.contains(query);
                    })
                    .toList();
        }

        // 2. Replace encrypted text with plain text temporarily
        // This only affects what is sent to HTML; data remains encrypted in the database
        encryptedList.forEach(cred -> {
            try {
                String plain = entryService.decrypt(cred.getPasswordEncrypted(), userService.getLoggedUser());
                cred.setPasswordEncrypted(plain); // Reuse the field only for rendering in HTML
            } catch (Exception e) {
                cred.setPasswordEncrypted("Error al descifrar");
            }
            cred.setImageUrl(imageService.getCredentialImageUrl(cred.getId()));
        });

        model.addAttribute("credentials", encryptedList);
        model.addAttribute("q", q == null ? "" : q);
        return "passwords";
    }

    @GetMapping("/add-password")
    public String addPassword(Model model) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }
        // user already added by populateUser()
        return "add-password";
    }

    @PostMapping("/add-password")
    public String savePassword(
            @RequestParam String service,
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam(required = false) String url,
            @RequestParam(required = false) String note,
            @RequestParam(required = false, name = "credentialImage") MultipartFile credentialImage,
            Model model
    ) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }

        String normalizedService = service == null ? "" : service.trim();
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPassword = password == null ? "" : password.trim();

        if (normalizedService.isBlank() || normalizedUsername.isBlank() || normalizedPassword.isBlank()) {
            model.addAttribute("error", "Debes completar servicio, usuario y contraseña.");
            return "add-password";
        }

        Credential c = new Credential();
        c.setSiteName(normalizedService);
        c.setUsername(normalizedUsername);
        c.setSiteUrl(url);
        c.setNote(note);
        Credential saved;
        try {
            saved = entryService.save(c, normalizedPassword);
        } catch (IllegalStateException e) {
            if (EntryService.FREE_PLAN_LIMIT_MESSAGE.equals(e.getMessage())) {
                model.addAttribute("error", e.getMessage());
                return "add-password";
            }
            throw e;
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "add-password";
        }
        String imageError = imageService.saveCredentialImage(saved.getId(), credentialImage, userService.getLoggedUser().get());
        if (imageError != null) {
            model.addAttribute("error", imageError);
            return "add-password";
        }
        return "redirect:/passwords";
    }

    @GetMapping("/info-passwords")
    public String infoPasswords(Model model, @RequestParam(required = false) Long id) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }

        if (id != null) {
            Optional<Credential> credOpt = entryService.findById(id);
            if (credOpt.isPresent()) {
                Credential cred = credOpt.get();
                // ensure the credential belongs to current user
                User current = userService.getLoggedUser().get();
                if (cred.getUser() != null && cred.getUser().getId().equals(current.getId())) {
                    model.addAttribute("credential", cred);
                    model.addAttribute("decryptedPassword", entryService.decrypt(cred.getPasswordEncrypted(), userService.getLoggedUser()));
                    model.addAttribute("credentialImageUrl", imageService.getCredentialImageUrl(cred.getId()));
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
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }
        Optional<Credential> credOpt = entryService.findById(id);
        if (credOpt.isPresent()) {
            Credential cred = credOpt.get();
            User current = userService.getLoggedUser().get();
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
                @RequestParam(required = false) String note,
                @RequestParam(required = false, name = "credentialImage") MultipartFile credentialImage,
                Model model
    ) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }

        String normalizedService = service == null ? "" : service.trim();
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedPassword = password == null ? "" : password.trim();

        if (normalizedService.isBlank() || normalizedUsername.isBlank() || normalizedPassword.isBlank()) {
            model.addAttribute("error", "Debes completar servicio, usuario y contraseña.");
            model.addAttribute("decryptedPassword", normalizedPassword);
            return "info-passwords";
        }

        Optional<Credential> existing = entryService.findById(id);
        if (existing.isEmpty()) {
            return "redirect:/passwords";
        }

        Credential c = existing.get();
        User current = userService.getLoggedUser().get();
        if (c.getUser() == null || !c.getUser().getId().equals(current.getId())) {
            return "redirect:/passwords";
        }

        c.setSiteName(normalizedService);
        c.setUsername(normalizedUsername);
        c.setSiteUrl(url);
        c.setNote(note);

        Credential saved;
        try {
            saved = entryService.save(c, normalizedPassword);
        } catch (IllegalArgumentException e) {
            model.addAttribute("credential", c);
            model.addAttribute("decryptedPassword", normalizedPassword);
            model.addAttribute("credentialImageUrl", imageService.getCredentialImageUrl(c.getId()));
            model.addAttribute("error", e.getMessage());
            return "info-passwords";
        }
        String imageError = imageService.saveCredentialImage(saved.getId(), credentialImage, current);
        if (imageError != null) {
            model.addAttribute("credential", saved);
            model.addAttribute("decryptedPassword", normalizedPassword);
            model.addAttribute("credentialImageUrl", imageService.getCredentialImageUrl(saved.getId()));
            model.addAttribute("error", imageError);
            return "info-passwords";
        }

        return "redirect:/passwords";
    }

    private String sanitizeRedirectTarget(String redirectTo) {
        if (redirectTo == null || redirectTo.isBlank()) {
            return "";
        }
        if (!redirectTo.startsWith("/") || redirectTo.startsWith("//")) {
            return "";
        }
        return redirectTo;
    }

}