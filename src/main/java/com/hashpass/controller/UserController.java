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

    public UserController(AuthService authService, UserSession userSession, ImageService imageService,
            UserRepository userRepository) {
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
            @RequestParam(required = false) String redirectTo,
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
        model.addAttribute("redirectTo", sanitizeRedirectTarget(redirectTo));
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
        redirectAttributes.addFlashAttribute("timeoutSuccess",
                "Timeout de seguridad actualizado a " + timeoutMinutes + " minutos.");
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
    public String admin(@RequestParam(required = false) String q,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String payment,
            @RequestParam(required = false, name = "regOrder") String regOrder,
            @RequestParam(required = false, name = "sortUser") String sortUser,
            Model model) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        if (!userSession.getUser().isAdmin()) {
            return "redirect:/error/403";
        }
        var users = userRepository.findAll();

        // total credentials across all users
        int passwordsCount = users.stream()
                .mapToInt(u -> u.getCredentials() == null ? 0 : u.getCredentials().size())
                .sum();

        // apply filters
        java.util.stream.Stream<com.hashpass.model.User> stream = users.stream();
        if (q != null && !q.isBlank()) {
            String qq = q.toLowerCase();
            stream = stream.filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(qq))
                    || (u.getName() != null && u.getName().toLowerCase().contains(qq)));
        }
        if (plan != null && !plan.isBlank() && !plan.equalsIgnoreCase("todos")) {
            String planLower = plan.toLowerCase();
            stream = stream.filter(u -> {
                String userPlan = u.getPlan() == null ? "gratuito" : u.getPlan().getName().toLowerCase();
                return userPlan.contains(planLower);
            });
        }
        if (payment != null && !payment.isBlank() && !payment.equalsIgnoreCase("todos")) {
            String paymentLower = payment.toLowerCase();
            // currently paymentStatus is derived/static; filter against computed string
            stream = stream.filter(u -> {
                String status = "Activo"; // placeholder logic
                return status.toLowerCase().contains(paymentLower);
            });
        }

        java.util.List<com.hashpass.model.User> filtered = stream.collect(java.util.stream.Collectors.toList());

        // sort: optionally by user (email) or by registration date
        java.util.Comparator<java.time.LocalDateTime> nullsLastDate = java.util.Comparator
                .nullsLast(java.util.Comparator.naturalOrder());
        java.util.Comparator<com.hashpass.model.User> byCreatedAt = java.util.Comparator
                .comparing((com.hashpass.model.User u) -> u.getCreatedAt(), nullsLastDate);

        java.util.Comparator<com.hashpass.model.User> finalComp = null;
        java.util.Comparator<com.hashpass.model.User> byEmail = java.util.Comparator
                .comparing(u -> u.getEmail() == null ? "" : u.getEmail().toLowerCase());

        if (sortUser != null && !sortUser.isBlank()) {
            finalComp = sortUser.equalsIgnoreCase("asc") ? byEmail : byEmail.reversed();
        }

        if (regOrder != null && !regOrder.isBlank()) {
            java.util.Comparator<com.hashpass.model.User> regComp = regOrder.equalsIgnoreCase("asc") ? byCreatedAt
                    : byCreatedAt.reversed();
            finalComp = (finalComp == null) ? regComp : finalComp.thenComparing(regComp);
        }

        if (finalComp == null) {
            finalComp = byCreatedAt.reversed();
        }

        filtered.sort(finalComp);

        var list = filtered.stream().map(u -> {
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
        model.addAttribute("passwordsCount", passwordsCount);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("selectedPlan", plan == null ? "todos" : plan);
        model.addAttribute("selectedPayment", payment == null ? "todos" : payment);
        model.addAttribute("regOrder", regOrder == null ? "desc" : regOrder);
        model.addAttribute("sortUser", sortUser == null ? "" : sortUser);

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
        model.addAttribute("detailCreatedAt",
                u.getCreatedAt() == null ? "-" : u.getCreatedAt().toLocalDate().toString());
        model.addAttribute("detailCredentialsCount", u.getCredentials() == null ? 0 : u.getCredentials().size());
        model.addAttribute("detailProfileImageUrl", imageService.getProfileImageUrl(u));

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