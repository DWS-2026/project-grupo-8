package com.hashpass.controller.web;

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

import jakarta.servlet.http.HttpServletRequest;

import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.security.HtmlSanitizer;
import com.hashpass.service.ImageService;
import com.hashpass.service.PlanService;
import com.hashpass.service.UserService;
import com.hashpass.service.DatabaseInitializer;

@Controller
public class PlanController {

    private final UserService userService;

    private final ImageService imageService;
    private final PlanService planService;
    private final HtmlSanitizer htmlSanitizer;

    public PlanController(ImageService imageService,
            PlanService planService, UserService userService, HtmlSanitizer htmlSanitizer) {
        this.imageService = imageService;
        this.planService = planService;
        this.userService = userService;
        this.htmlSanitizer = htmlSanitizer;
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
        Long currentPlanId = userService.getLoggedUser()
                .map(User::getPlan)
                .map(Plan::getId)
                .orElse(null);

        List<Map<String, Object>> allPlans = planService.findAll().stream().map(plan -> {
            Map<String, Object> planView = new HashMap<>();
            BigDecimal price = plan.getPriceMonthly() == null ? BigDecimal.ZERO : plan.getPriceMonthly();

            planView.put("id", plan.getId());
            planView.put("name", plan.getName());
            planView.put("description", plan.getDescription());
            planView.put("priceMonthly", price);
            planView.put("isFree", price.compareTo(BigDecimal.ZERO) <= 0);
            planView.put("isCurrentPlan", currentPlanId != null && currentPlanId.equals(plan.getId()));
            return planView;
        }).toList();

        model.addAttribute("allPlans", allPlans);
        return "plan";
    }

    @GetMapping("/payment")
    public String payment(@RequestParam(required = false) String plan,
                          @RequestParam(required = false) String discountCode,
                          @RequestParam(required = false) String discountError,
                          HttpServletRequest request,
                          Model model) {
        Plan selectedPlan = findPlanFromInput(plan)
            .or(() -> planService.findByName("Premium"))
                .orElse(null);

        if (selectedPlan == null) {
            return "redirect:/plan";
        }

        BigDecimal price = selectedPlan.getPriceMonthly() == null ? BigDecimal.ZERO : selectedPlan.getPriceMonthly();
        BigDecimal baseDiscount = getDiscountForPlan(selectedPlan.getName(), price);
        DiscountApplication discountApplication = resolveDiscountApplication(discountCode, request, price);

        if (discountApplication.lockedOut()) {
            return "redirect:/login?discountLocked=1";
        }

        // If a coupon is applied, use only the coupon discount (10% of original price)
        // If no coupon, use the base discount (plan-specific automatic discount)
        BigDecimal discount;
        if (discountApplication.couponDiscount().compareTo(BigDecimal.ZERO) > 0) {
            discount = discountApplication.couponDiscount();
        } else {
            discount = baseDiscount;
        }
        boolean applied = discountApplication.couponDiscount().compareTo(BigDecimal.ZERO) > 0;

        BigDecimal total = price.subtract(discount == null ? BigDecimal.ZERO : discount);

        model.addAttribute("paymentPlanKey", String.valueOf(selectedPlan.getId()));
        model.addAttribute("paymentPlanName", "Plan " + selectedPlan.getName());
        model.addAttribute("paymentBillingLabel", "Facturación mensual");
        model.addAttribute("paymentPrice", formatEur(price));
        model.addAttribute("paymentDiscount", "-" + formatEur(discount));
        model.addAttribute("paymentTotal", formatEur(total));
        model.addAttribute("discountCode", discountCode == null ? "" : discountCode.trim());
        model.addAttribute("discountApplied", applied);
        model.addAttribute("discountError", discountError != null || discountApplication.invalid());

        return "payment";
    }

    @PostMapping("/payment/confirm")
    public String confirmPayment(@RequestParam String plan,
                                 @RequestParam(required = false) String discountCode,
                                 HttpServletRequest request) {
        Plan targetPlan = findPlanFromInput(plan).orElse(null);

        if (targetPlan == null) {
            return "redirect:/payment";
        }

        if (!userService.getLoggedUser().isPresent()) {
            BigDecimal price = targetPlan.getPriceMonthly() == null ? BigDecimal.ZERO : targetPlan.getPriceMonthly();
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                return "redirect:/register?plan=" + targetPlan.getId();
            }
            // store prepaid plan id and preserve discount info in session for after-register flow
            request.getSession().setAttribute("prepaidPlanId", targetPlan.getId());
            if (discountCode != null && !discountCode.isBlank()) {
                request.getSession().setAttribute("prepaidDiscountCode", discountCode.trim());
            }
            return "redirect:/register?plan=" + targetPlan.getId() + "&paid=1";
        }

        // If a discount code was provided, validate it on the backend; if invalid, send back to payment
        if (discountCode != null && !discountCode.isBlank()) {
            if (!DatabaseInitializer.DEFAULT_DISCOUNT_CODE.equalsIgnoreCase(discountCode.trim())) {
                boolean lockedOut = registerDiscountFailure(request);
                if (lockedOut) {
                    return "redirect:/login?discountLocked=1";
                }
                return "redirect:/payment?plan=" + targetPlan.getId() + "&discountCode=" + urlEncode(discountCode) + "&discountError=1";
            }
            resetDiscountFailureCounter(request);
        }

        if (userService.updateLoggedUserPlan(targetPlan).isEmpty()) {
            userService.logout();
            return "redirect:/login";
        }

        return "redirect:/dashboard";
    }

    private DiscountApplication resolveDiscountApplication(String discountCode, HttpServletRequest request, BigDecimal price) {
        if (discountCode == null || discountCode.isBlank()) {
            return new DiscountApplication(BigDecimal.ZERO, false, false);
        }

        String normalized = discountCode.trim();
        if (DatabaseInitializer.DEFAULT_DISCOUNT_CODE.equalsIgnoreCase(normalized)) {
            resetDiscountFailureCounter(request);
            BigDecimal couponDiscount = price.multiply(DatabaseInitializer.DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
            return new DiscountApplication(couponDiscount, false, false);
        }

        boolean lockedOut = registerDiscountFailure(request);
        return new DiscountApplication(BigDecimal.ZERO, true, lockedOut);
    }

    private boolean registerDiscountFailure(HttpServletRequest request) {
        int attempts = getDiscountFailureCounter(request) + 1;
        request.getSession().setAttribute(DatabaseInitializer.DISCOUNT_FAILS_SESSION_KEY, attempts);
        if (attempts > 5) {
            request.getSession().invalidate();
            userService.logout();
            return true;
        }
        return false;
    }

    private void resetDiscountFailureCounter(HttpServletRequest request) {
        request.getSession().setAttribute(DatabaseInitializer.DISCOUNT_FAILS_SESSION_KEY, 0);
    }

    private int getDiscountFailureCounter(HttpServletRequest request) {
        Object value = request.getSession().getAttribute(DatabaseInitializer.DISCOUNT_FAILS_SESSION_KEY);
        if (value instanceof Integer) {
            return (Integer) value;
        }
        return 0;
    }

    private String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private record DiscountApplication(BigDecimal couponDiscount, boolean invalid, boolean lockedOut) {}

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
    // ADMIN ENDPOINTS - PLAN CRUD
    // =====================================================

    /**
     * Shows the list of all plans (admins only)
     */
    @GetMapping("/admin/plans")
    public String adminPlans(Model model) {
        // Verify admin access
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        List<Plan> plans = planService.findAll();
        model.addAttribute("plans", plans);
        return "admin_plans";
    }

    /**
     * Shows the form to add a new plan (admins only)
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
     * Creates a new plan (admins only)
     */
    @PostMapping("/admin/plan/add")
    public String addPlan(@RequestParam String name, @RequestParam BigDecimal priceMonthly,
            @RequestParam String description, RedirectAttributes redirectAttributes) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        String sanitizedName = htmlSanitizer.sanitizePlainText(name);
        String sanitizedDescription = htmlSanitizer.sanitizeOptionalPlainText(description);

        // Validate fields
        if (sanitizedName == null || sanitizedName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del plan es requerido.");
            return "redirect:/admin/plan/add";
        }

        if (planService.findByName(sanitizedName).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ya existe un plan con ese nombre.");
            return "redirect:/admin/plan/add";
        }

        if (priceMonthly == null || priceMonthly.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor o igual a 0.");
            return "redirect:/admin/plan/add";
        }

        Plan newPlan = new Plan();
        newPlan.setName(sanitizedName);
        newPlan.setPriceMonthly(priceMonthly);
        newPlan.setDescription(sanitizedDescription == null ? "" : sanitizedDescription);

        planService.save(newPlan);
        redirectAttributes.addFlashAttribute("success", "Plan '" + sanitizedName + "' creado exitosamente.");

        return "redirect:/admin/plans";
    }

    /**
     * Shows the form to edit an existing plan (admins only)
     */
    @GetMapping("/admin/plan/edit/{id}")
    public String showEditPlanForm(@PathVariable Long id, Model model) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        Optional<Plan> planOpt = planService.findById(id);
        if (planOpt.isEmpty()) {
            return "redirect:/admin/plans";
        }

        model.addAttribute("plan", planOpt.get());
        return "edit_plan";
    }

    /**
     * Updates an existing plan (admins only)
     */
    @PostMapping("/admin/plan/edit/{id}")
    public String editPlan(@PathVariable Long id, @RequestParam String name, @RequestParam BigDecimal priceMonthly,
            @RequestParam String description, RedirectAttributes redirectAttributes) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        Optional<Plan> planOpt = planService.findById(id);
        if (planOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Plan no encontrado.");
            return "redirect:/admin/plans";
        }

        Plan plan = planOpt.get();

        String sanitizedName = htmlSanitizer.sanitizePlainText(name);
        String sanitizedDescription = htmlSanitizer.sanitizeOptionalPlainText(description);

        // Validate fields
        if (sanitizedName == null || sanitizedName.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El nombre del plan es requerido.");
            return "redirect:/admin/plan/edit/" + id;
        }

        // Validate that the new name is unique (unless it is the same plan)
        Optional<Plan> existingPlan = planService.findByName(sanitizedName);
        if (existingPlan.isPresent() && existingPlan.get().getId() != id) {
            redirectAttributes.addFlashAttribute("error", "Ya existe otro plan con ese nombre.");
            return "redirect:/admin/plan/edit/" + id;
        }

        if (priceMonthly == null || priceMonthly.compareTo(BigDecimal.ZERO) < 0) {
            redirectAttributes.addFlashAttribute("error", "El precio debe ser mayor o igual a 0.");
            return "redirect:/admin/plan/edit/" + id;
        }

        plan.setName(sanitizedName);
        plan.setPriceMonthly(priceMonthly);
        plan.setDescription(sanitizedDescription == null ? "" : sanitizedDescription);

        planService.save(plan);
        redirectAttributes.addFlashAttribute("success", "Plan '" + sanitizedName + "' actualizado exitosamente.");

        return "redirect:/admin/plans";
    }

    /**
     * Deletes a plan (admins only)
     */
    @PostMapping("/admin/plan/delete/{id}")
    public String deletePlan(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        if (!isAdmin()) {
            return "redirect:/admin";
        }

        Optional<Plan> planOpt = planService.findById(id);
        if (planOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Plan no encontrado.");
            return "redirect:/admin/plans";
        }

        Plan plan = planOpt.get();

        // Verify there are no users assigned to this plan
        if (!plan.getUsers().isEmpty()) {
            redirectAttributes.addFlashAttribute("error",
                    "No se puede eliminar el plan porque hay usuarios asignados a él.");
            return "redirect:/admin/plans";
        }

        String planName = plan.getName();
        planService.delete(plan);
        redirectAttributes.addFlashAttribute("success", "Plan '" + planName + "' eliminado exitosamente.");

        return "redirect:/admin/plans";
    }

    // =====================================================
    // HELPER METHODS
    // =====================================================

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
            return planService.findById(planId);
        } catch (NumberFormatException ignored) {
            // If it is not a numeric id, try resolving by name or alias.
        }

        Optional<Plan> directMatch = planService.findByName(trimmed);
        if (directMatch.isPresent()) {
            return directMatch;
        }

        String normalized = normalizePlan(trimmed);
        if ("free".equals(normalized)) {
            return planService.findByName("Gratuito");
        }
        if ("platinum".equals(normalized)) {
                return planService.findByName("Platinum")
                    .or(() -> planService.findByName("Platino"));
        }
        if ("premium".equals(normalized)) {
            return planService.findByName("Premium");
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
     * Checks whether the current user is an administrator
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