package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hashpass.service.UserSession;

@Controller
public class PlanController {

    private final UserSession userSession;

    public PlanController(UserSession userSession) {
        this.userSession = userSession;
    }

    @GetMapping("/plan")
    public String plan(Model model) {
        return requireLogin(model, "plan");
    }

    @GetMapping("/payment")
    public String payment(Model model) {
        return requireLogin(model, "payment");
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