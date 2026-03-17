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
import com.hashpass.repository.UserRepository;
import com.hashpass.service.AuthService;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserSession;

@Controller
public class UserController {

    private final AuthService authService;
    private final UserSession userSession;
    private final ImageService imageService;
    private final UserRepository userRepository;

    @ModelAttribute("user")
    public User populateUser() {
        return userSession.getUser();
    }

    @ModelAttribute("profileImageUrl")
    public String populateProfileImageUrl() {
        return imageService.getProfileImageUrl(userSession.getUser());
    }

    public UserController(AuthService authService, UserSession userSession, ImageService imageService, UserRepository userRepository) {
        this.authService = authService;
        this.userSession = userSession;
        this.imageService = imageService;
        this.userRepository = userRepository;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String deleted,
                        @RequestParam(required = false) String logout,
                        @RequestParam(required = false) String expired,
                        @RequestParam(required = false) String error,
                        Model model) {
        if (deleted != null) {
            model.addAttribute("success", "Tu cuenta se ha eliminado definitivamente.");
        }
        if (logout != null) {
            model.addAttribute("success", "Sesión cerrada correctamente.");
        }
        if (expired != null && logout == null) {
            model.addAttribute("expired", "Tu sesión ha expirado por inactividad. Vuelve a iniciar sesión.");
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

    @PostMapping("/security-user/timeout")
    public String updateSecurityTimeout(@RequestParam Integer timeoutMinutes,
                                        HttpServletRequest request,
                                        RedirectAttributes redirectAttributes) {
        if (timeoutMinutes == null || timeoutMinutes < 1 || timeoutMinutes > 120) {
            redirectAttributes.addFlashAttribute("timeoutError", "El timeout debe estar entre 1 y 120 minutos.");
            return "redirect:/security-user";
        }

        User currentUser = userSession.getUser();
        currentUser.setSecurityTimeoutMinutes(timeoutMinutes);
        userRepository.save(currentUser);

        request.getSession().setMaxInactiveInterval(timeoutMinutes * 60);
        redirectAttributes.addFlashAttribute("timeoutSuccess", "Timeout de seguridad actualizado a " + timeoutMinutes + " minutos.");
        return "redirect:/security-user";
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
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        if (!userSession.getUser().isAdmin()) {
            return "redirect:/error/403";
        }

        var users = userRepository.findAll();

        var list = users.stream().map(u -> {
            java.util.Map<String, Object> m = new java.util.HashMap<>();
            m.put("id", u.getId());
            m.put("email", u.getEmail());
            m.put("name", u.getName());
            m.put("credentialsCount", u.getCredentials() == null ? 0 : u.getCredentials().size());
            m.put("plan", u.getPlan() == null ? "Gratuito" : u.getPlan().getName());
            m.put("createdAt", u.getCreatedAt() == null ? "-" : u.getCreatedAt().toLocalDate().toString());
            m.put("paymentStatus", "Activo");
            return m;
        }).toList();

        model.addAttribute("usersList", list);
        model.addAttribute("usersCount", list.size());

        return "admin";
    }

    @GetMapping("/admin-user-detail")
    public String adminUserDetail(@RequestParam(required = false) Long id, Model model) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        if (!userSession.getUser().isAdmin()) {
            return "redirect:/error/403";
        }

        if (id == null) {
            return "redirect:/admin";
        }

        var opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            return "redirect:/admin";
        }

        var u = opt.get();
        model.addAttribute("detailId", u.getId());
        model.addAttribute("detailName", u.getName());
        model.addAttribute("detailEmail", u.getEmail());
        model.addAttribute("detailPlan", u.getPlan() == null ? "Gratuito" : u.getPlan().getName());
        model.addAttribute("detailCreatedAt", u.getCreatedAt() == null ? "-" : u.getCreatedAt().toLocalDate().toString());
        model.addAttribute("detailCredentialsCount", u.getCredentials() == null ? 0 : u.getCredentials().size());

        return "admin_user_detail";
    }

    @PostMapping("/admin/delete-user")
    public String deleteUser(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        if (!userSession.isLogged() || !userSession.getUser().isAdmin()) {
            return "redirect:/error/403";
        }

        Long currentId = userSession.getUser().getId();
        if (currentId != null && currentId.equals(id)) {
            redirectAttributes.addFlashAttribute("error", "No puedes eliminar tu propia cuenta.");
            return "redirect:/admin";
        }

        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Usuario eliminado correctamente.");
        } else {
            redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
        }

        return "redirect:/admin";
    }

    private String requireLogin(Model model, String view) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        return view;
    }
}