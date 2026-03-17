package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hashpass.model.User;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserSession;

@Controller
public class ReviewController {

    private final UserSession userSession;
    private final ImageService imageService;

    public ReviewController(UserSession userSession, ImageService imageService) {
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

    @GetMapping("/reviews")
    public String reviews(Model model) {
        return "reviews";
    }
}