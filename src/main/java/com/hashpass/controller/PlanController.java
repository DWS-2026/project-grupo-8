package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hashpass.model.User;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserSession;

@Controller
public class PlanController {

    private final UserSession userSession;
    private final ImageService imageService;

    public PlanController(UserSession userSession, ImageService imageService) {
        this.userSession = userSession;
        this.imageService = imageService;
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

    @GetMapping("/plan")
    public String plan(Model model) {
        return "plan";
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