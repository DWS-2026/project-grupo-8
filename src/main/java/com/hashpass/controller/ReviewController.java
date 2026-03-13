package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.hashpass.service.UserSession;

@Controller
public class ReviewController {

    private final UserSession userSession;

    public ReviewController(UserSession userSession) {
        this.userSession = userSession;
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        return requireLogin(model, "reviews");
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