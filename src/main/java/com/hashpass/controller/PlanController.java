package com.hashpass.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.hashpass.service.ImageService;
import com.hashpass.service.UserService;

@Controller
public class PlanController {

    private final UserService userService;

    private final ImageService imageService;
    private final PlanRepository planRepository;

    public PlanController(ImageService imageService,
            PlanRepository planRepository, UserService userService) {
        this.imageService = imageService;
        this.planRepository = planRepository;
        this.userService = userService;
    }

    @ModelAttribute("user")
    public User populateUser() {
        return userService.getLoggedUser().orElse(null);
    }

    @ModelAttribute("profileImageUrl")
    public String populateProfileImageUrl() {
        return imageService.getProfileImageUrl(userService.getLoggedUser());
    }

    @ModelAttribute("isLogged")
    public boolean populateIsLogged() {
        return userService.getLoggedUser().isPresent();
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
        List<Map<String, Object>> allPlans = planRepository.findAll().stream().map(plan -> {
            Map<String, Object> planView = new HashMap<>();
            BigDecimal price = plan.getPriceMonthly() == null ? BigDecimal.ZERO : plan.getPriceMonthly();

            planView.put("id", plan.getId());
            planView.put("name", plan.getName());
            planView.put("description", plan.getDescription());
            planView.put("priceMonthly", price);
            planView.put("isFree", price.compareTo(BigDecimal.ZERO) <= 0);
            return planView;
        }).toList();

        model.addAttribute("allPlans", allPlans);
        return "plan";
    }

    @GetMapping("/payment")
    public String payment(@RequestParam(required = false) String plan, Model model) {
        Plan selectedPlan = findPlanFromInput(plan)
                .or(() -> planRepository.findByName("Premium"))
                .orElse(null);

        if (selectedPlan == null) {
            return "redirect:/plan";
        }

        BigDecimal price = selectedPlan.getPriceMonthly() == null ? BigDecimal.ZERO : selectedPlan.getPriceMonthly();
        BigDecimal discount = getDiscountForPlan(selectedPlan.getName(), price);
        BigDecimal total = price.subtract(discount);

        model.addAttribute("paymentPlanKey", String.valueOf(selectedPlan.getId()));
        model.addAttribute("paymentPlanName", "Plan " + selectedPlan.getName());
        model.addAttribute("paymentBillingLabel", "Facturación mensual");
        model.addAttribute("paymentPrice", formatEur(price));
        model.addAttribute("paymentDiscount", "-" + formatEur(discount));
        model.addAttribute("paymentTotal", formatEur(total));

        return requireLogin(model, "payment");
    }

    @PostMapping("/payment/confirm")
    public String confirmPayment(@RequestParam String plan) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }

        Plan targetPlan = findPlanFromInput(plan).orElse(null);

        if (targetPlan == null) {
            return "redirect:/payment";
        }

        if (userService.updateLoggedUserPlan(targetPlan).isEmpty()) {
            userService.logout();
            return "redirect:/login";
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/plan/select")
    public String selectPlan(@RequestParam String plan) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login";
        }

        Plan targetPlan = findPlanFromInput(plan).orElse(null);

        if (targetPlan == null) {
            return "redirect:/plan";
        }

        if (userService.updateLoggedUserPlan(targetPlan).isEmpty()) {
            userService.logout();
            return "redirect:/login";
        }

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
        if (userService.getLoggedUser().isEmpty()) {
            return "redirect:/login";
        }
        // user already added by populateUser()
        return view;
    }

    private boolean hasCurrentPlan(String planName) {
        Optional<User> userOpt = userService.getLoggedUser();
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        if (user.getPlan() == null || user.getPlan().getName() == null) {
            return false;
        }
        return user.getPlan().getName().equalsIgnoreCase(planName);
    }

    private String normalizePlan(String plan) {
        if (plan == null || plan.isBlank()) {
            return "";
        }

        String lower = plan.trim().toLowerCase();
        if ("free".equals(lower) || "gratuito".equals(lower) || "gratis".equals(lower)) {
            return "free";
        }
        if ("platinum".equals(lower) || "platino".equals(lower)) {
            return "platinum";
        }
        if ("premium".equals(lower)) {
            return "premium";
        }
        return lower;
    }

    private Optional<Plan> findPlanFromInput(String rawPlan) {
        if (rawPlan == null || rawPlan.isBlank()) {
            return Optional.empty();
        }

        String trimmed = rawPlan.trim();
        try {
            Long planId = Long.parseLong(trimmed);
            return planRepository.findById(planId);
        } catch (NumberFormatException ignored) {
            // Si no es id numérico, intentar resolver por nombre o alias.
        }

        Optional<Plan> directMatch = planRepository.findByName(trimmed);
        if (directMatch.isPresent()) {
            return directMatch;
        }

        String normalized = normalizePlan(trimmed);
        if ("free".equals(normalized)) {
            return planRepository.findByName("Gratuito");
        }
        if ("platinum".equals(normalized)) {
            return planRepository.findByName("Platinum")
                    .or(() -> planRepository.findByName("Platino"));
        }
        if ("premium".equals(normalized)) {
            return planRepository.findByName("Premium");
        }

        return Optional.empty();
    }

    private BigDecimal getDiscountForPlan(String planName, BigDecimal price) {
        if (planName == null) {
            return BigDecimal.ZERO;
        }
        String normalized = normalizePlan(planName);
        if ("platinum".equals(normalized)) {
            return new BigDecimal("2.99");
        }
        if ("premium".equals(normalized)) {
            return new BigDecimal("1.99");
        }
        return BigDecimal.ZERO;
    }

    private String formatEur(BigDecimal amount) {
        BigDecimal safeAmount = amount == null ? BigDecimal.ZERO : amount;
        return safeAmount.setScale(2, RoundingMode.HALF_UP).toPlainString() + "€";
    }

    /**
     * Verifica si el usuario actual es administrador
     */
    private boolean isAdmin() {
        Optional<User> userOpt = userService.getLoggedUser();
        if (userOpt.isEmpty()) {
            return false;
        }
        User user = userOpt.get();
        return user != null && user.isAdmin();
    }
}