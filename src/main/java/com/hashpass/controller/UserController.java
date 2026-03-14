package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.security.core.context.SecurityContextHolder;

import com.hashpass.model.User;
import com.hashpass.service.AuthService;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserSession;

@Controller
public class UserController {

    private final AuthService authService;
    private final UserSession userSession;
    private final ImageService imageService;

    @ModelAttribute("user")
    public User populateUser() {
        return userSession.getUser();
    }

    @ModelAttribute("profileImageUrl")
    public String populateProfileImageUrl() {
        return imageService.getProfileImageUrl(userSession.getUser());
    }

    public UserController(AuthService authService, UserSession userSession, ImageService imageService) {
        this.authService = authService;
        this.userSession = userSession;
        this.imageService = imageService;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String deleted,
                        @RequestParam(required = false) String error,
                        Model model) {
        if (deleted != null) {
            model.addAttribute("success", "Tu cuenta se ha eliminado definitivamente.");
        }
        if (error != null) {
            model.addAttribute("error", "No existe ninguna cuenta con ese correo.");
        }
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
            @RequestParam(required = false, name = "avatar") MultipartFile avatar,
            Model model) {

        if (authService.isEmailRegistered(email)) {
            model.addAttribute("error", "El correo ya está registrado.");
            return "register";
        }

        User registeredUser = authService.registerUser(name, email, password);

        if (avatar != null && !avatar.isEmpty()) {
            imageService.saveProfileImage(registeredUser, avatar);
        }

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

    @PostMapping("/config-user/avatar")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile avatar,
                               RedirectAttributes redirectAttributes) {
        User currentUser = userSession.getUser();
        String error = imageService.saveProfileImage(currentUser, avatar);
        if (error == null) {
            redirectAttributes.addFlashAttribute("success", "Foto de perfil actualizada correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", error);
        }
        return "redirect:/config-user";
    }

    @PostMapping("/config-user")
    public String changeEmail(@RequestParam String masterPass,
                              @RequestParam String newEmail,
                              Model model) {
        User currentUser = userSession.getUser();
        String error = authService.changeEmail(currentUser, masterPass, newEmail);
        if (error == null) {
            // Actualizar el user en sesión con el nuevo email
            currentUser.setEmail(newEmail);
            model.addAttribute("success", "Correo electrónico cambiado exitosamente.");
        } else {
            model.addAttribute("error", error);
        }
        return "config_user";
    }

    @GetMapping("/security-user")
    public String securityUser(Model model) {
        return requireLogin(model, "security_user");
    }

    @PostMapping("/security-user")
    public String changeMasterPassword(@RequestParam String currentPass,
                                       @RequestParam String newPass,
                                       @RequestParam String confirmPass,
                                       Model model) {
        User currentUser = userSession.getUser();
        String error = authService.changeMasterPassword(currentUser, currentPass, newPass, confirmPass);
        if (error == null) {
            model.addAttribute("success", "Contraseña maestra actualizada correctamente.");
        } else {
            model.addAttribute("error", error);
        }
        return "security_user";
    }

    @PostMapping("/security-user/delete-account")
    public String deleteAccount(@RequestParam String deleteAccountPass,
                                Model model,
                                HttpServletRequest request) {
        User currentUser = userSession.getUser();
        String error = authService.deleteAccount(currentUser, deleteAccountPass);
        if (error != null) {
            model.addAttribute("error", error);
            return "security_user";
        }

        userSession.logout();
        SecurityContextHolder.clearContext();
        request.getSession().invalidate();
        return "redirect:/login?deleted=1";
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