package com.hashpass.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.repository.PlanRepository;
import com.hashpass.repository.UserRepository;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserService;
import com.hashpass.service.UserSession;

@Controller
public class PlanController {

    private final UserSession userSession;
    private final UserService userService;

    private final ImageService imageService;
    private final UserRepository userRepository;
    private final PlanRepository planRepository;

    public PlanController(UserSession userSession, ImageService imageService, UserRepository userRepository,
            PlanRepository planRepository, UserService userService) {
        this.userSession = userSession;
        this.imageService = imageService;
        this.userRepository = userRepository;
        this.planRepository = planRepository;
        this.userService = userService;
    }

    @ModelAttribute("user")
    public User populateUser() {
        return userSession.getUser();
    }

    @ModelAttribute("profileImageUrl")
    public String populateProfileImageUrl() {
        return imageService.getProfileImageUrl(userSession.getUser());
    }

    @ModelAttribute("isLogged")
    public boolean populateIsLogged() {
        return userSession.isLogged();
    }

    @ModelAttribute("isFreePlan")
    public boolean populateIsFreePlan() {
        return hasCurrentPlan("Gratuito");
    }

    @ModelAttribute("isPremiumPlan")
    public boolean populateIsPremiumPlan() {
        return hasCurrentPlan("Premium");
    }

    @ModelAttribute("isPlatinumPlan")
    public boolean populateIsPlatinumPlan() {
        return hasCurrentPlan("Platinum") || hasCurrentPlan("Platino");
    }

    @GetMapping("/plan")
    public String plan(Model model) {
        // Obtener todos los planes de la base de datos y pasarlos a la vista
        List<Plan> allPlans = planRepository.findAll();
        model.addAttribute("allPlans", allPlans);
        return "plan";
    }

    @GetMapping("/payment")
    public String payment(@RequestParam(required = false) String plan, Model model) {
        String normalizedPlan = normalizePlan(plan);

        model.addAttribute("paymentPlanKey", normalizedPlan);

        if ("platinum".equals(normalizedPlan)) {
            model.addAttribute("paymentPlanName", "Plan Platinum");
            model.addAttribute("paymentBillingLabel", "Facturación mensual");
            model.addAttribute("paymentPrice", "9.99€");
            model.addAttribute("paymentDiscount", "-2.99€");
            model.addAttribute("paymentTotal", "7.00€");
        } else {
            model.addAttribute("paymentPlanName", "Plan Premium");
            model.addAttribute("paymentBillingLabel", "Facturación mensual");
            model.addAttribute("paymentPrice", "4.99€");
            model.addAttribute("paymentDiscount", "-1.99€");
            model.addAttribute("paymentTotal", "3.00€");
        }

        return requireLogin(model, "payment");
    }

    @PostMapping("/payment/confirm")
    public String confirmPayment(@RequestParam String plan) {
        if (!userSession.isLogged() || userSession.getUser() == null || userSession.getUser().getId() == null) {
            return "redirect:/login";
        }

        String normalizedPlan = normalizePlan(plan);
        String planName = "platinum".equals(normalizedPlan) ? "Platinum" : "Premium";

        Plan targetPlan = planRepository.findByName(planName)
                .or(() -> "Platinum".equals(planName) ? planRepository.findByName("Platino") : java.util.Optional.empty())
                .orElse(null);

        if (targetPlan == null) {
            return "redirect:/payment?plan=" + normalizedPlan;
        }

        Long userId = userSession.getUser().getId();
        User persistedUser = userRepository.findById(userId).orElse(null);
        if (persistedUser == null) {
            userSession.logout();
            return "redirect:/login";
        }

        persistedUser.setPlan(targetPlan);
        userRepository.save(persistedUser);

        // Mantener sesión sincronizada con el plan actualizado
        userSession.setUser(persistedUser);

        return "redirect:/dashboard";
    }

    @PostMapping("/plan/select")
    public String selectPlan(@RequestParam String plan) {
        if (!userSession.isLogged() || userSession.getUser() == null || userSession.getUser().getId() == null) {
            return "redirect:/login";
        }

        String normalizedPlan = normalizePlan(plan);
        String planName = "platinum".equals(normalizedPlan) ? "Platinum"
                : "free".equals(normalizedPlan) ? "Gratuito" : "Premium";

        Plan targetPlan = planRepository.findByName(planName)
                .or(() -> "Platinum".equals(planName) ? planRepository.findByName("Platino") : java.util.Optional.empty())
                .orElse(null);

        if (targetPlan == null) {
            return "redirect:/plan";
        }

        Long userId = userSession.getUser().getId();
        User persistedUser = userRepository.findById(userId).orElse(null);
        if (persistedUser == null) {
            userSession.logout();
            return "redirect:/login";
        }

        persistedUser.setPlan(targetPlan);
        userRepository.save(persistedUser);
        userSession.setUser(persistedUser);

        return "redirect:/dashboard";
    }

    // =====================================================
    // ENDPOINTS PARA ADMINISTRADORES - CRUD DE PLANES
    // =====================================================

    /**
     * Muestra la lista de todos los planes (solo administradores)
     */
    @GetMapping("/admin/plans")
    public String adminPlans(Model model) {
        // Verificar que sea admin
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        List<Plan> plans = planRepository.findAll();
        model.addAttribute("plans", plans);
        return "admin_plans";
    }

    /**
     * Muestra el formulario para agregar un nuevo plan (solo administradores)
     */
    @GetMapping("/admin/plan/add")
    public String showAddPlanForm(Model model) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        model.addAttribute("plan", new Plan());
        return "add_plan";
    }

    /**
     * Crea un nuevo plan (solo administradores)
     */
    @PostMapping("/admin/plan/add")
    public String addPlan(@RequestParam String name, @RequestParam BigDecimal priceMonthly,
            @RequestParam String description, RedirectAttributes redirectAttributes) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        // Validar que el plan no exista
        if (planRepository.findByName(name).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un plan con ese nombre.");
            return "redirect:/admin/plan/add";
        }

        // Validar campos
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del plan es requerido.");
            return "redirect:/admin/plan/add";
        }

        if (priceMonthly == null || priceMonthly.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor o igual a 0.");
            return "redirect:/admin/plan/add";
        }

        Plan newPlan = new Plan();
        newPlan.setName(name.trim());
        newPlan.setPriceMonthly(priceMonthly);
        newPlan.setDescription(description != null ? description.trim() : "");

        planRepository.save(newPlan);
        redirectAttributes.addFlashAttribute("success", "Plan '" + name + "' creado exitosamente.");

        return "redirect:/admin/plans";
    }

    /**
     * Muestra el formulario para editar un plan existente (solo administradores)
     */
    @GetMapping("/admin/plan/edit/{id}")
    public String showEditPlanForm(@PathVariable Long id, Model model) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        Optional<Plan> planOpt = planRepository.findById(id);
        if (planOpt.isEmpty()) {
            return "redirect:/admin/plans";
        }

        model.addAttribute("plan", planOpt.get());
        return "edit_plan";
    }

    /**
     * Actualiza un plan existente (solo administradores)
     */
    @PostMapping("/admin/plan/edit/{id}")
    public String editPlan(@PathVariable Long id, @RequestParam String name, @RequestParam BigDecimal priceMonthly,
            @RequestParam String description, RedirectAttributes redirectAttributes) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        Optional<Plan> planOpt = planRepository.findById(id);
        if (planOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Plan no encontrado.");
            return "redirect:/admin/plans";
        }

        Plan plan = planOpt.get();

        // Validar que el nuevo nombre no exista (excepto si es el mismo plan)
        Optional<Plan> existingPlan = planRepository.findByName(name);
        if (existingPlan.isPresent() && existingPlan.get().getId() != id) {
            redirectAttributes.addFlashAttribute("error", "Ya existe otro plan con ese nombre.");
            return "redirect:/admin/plan/edit/" + id;
        }

        // Validar campos
        if (name == null || name.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del plan es requerido.");
            return "redirect:/admin/plan/edit/" + id;
        }

        if (priceMonthly == null || priceMonthly.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor o igual a 0.");
            return "redirect:/admin/plan/edit/" + id;
        }

        plan.setName(name.trim());
        plan.setPriceMonthly(priceMonthly);
        plan.setDescription(description != null ? description.trim() : "");

        planRepository.save(plan);
        redirectAttributes.addFlashAttribute("success", "Plan '" + name + "' actualizado exitosamente.");

        return "redirect:/admin/plans";
    }

    /**
     * Elimina un plan (solo administradores)
     */
    @PostMapping("/admin/plan/delete/{id}")
    public String deletePlan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        Optional<Plan> planOpt = planRepository.findById(id);
        if (planOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Plan no encontrado.");
            return "redirect:/admin/plans";
        }

        Plan plan = planOpt.get();

        // Verificar que no haya usuarios con este plan
        if (!plan.getUsers().isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede eliminar el plan porque hay usuarios asignados a él.");
            return "redirect:/admin/plans";
        }

        String planName = plan.getName();
        planRepository.delete(plan);
        redirectAttributes.addFlashAttribute("success", "Plan '" + planName + "' eliminado exitosamente.");

        return "redirect:/admin/plans";
    }

    // =====================================================
    // MÉTODOS AUXILIARES
    // =====================================================

    private String requireLogin(Model model, String view) {
        if (!userSession.isLogged()) {
            return "redirect:/login";
        }
        // user already added by populateUser()
        return view;
    }

    private boolean hasCurrentPlan(String planName) {
        if (!userSession.isLogged()) {
            return false;
        } else {
            User user = userService.getLoggedUser();
            if (user == null || user.getPlan() == null || user.getPlan().getName() == null) {
                return false;
            }
            return user.getPlan().getName().equalsIgnoreCase(planName);
        }
    }

    private String normalizePlan(String plan) {
        if (plan == null) {
            return "premium";
        }

        String lower = plan.trim().toLowerCase();
        if ("free".equals(lower) || "gratuito".equals(lower) || "gratis".equals(lower)) {
            return "free";
        }
        if ("platinum".equals(lower) || "platino".equals(lower)) {
            return "platinum";
        }
        return "premium";
    }

    /**
     * Verifica si el usuario actual es administrador
     */
    private boolean isAdmin() {
        if (!userSession.isLogged()) {
            return false;
        }
        User user = userSession.getUser();
        return user != null && user.isAdmin();
    }
}