package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    // helper to require login and automatically supply user via @ModelAttribute
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
        }else{
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
}