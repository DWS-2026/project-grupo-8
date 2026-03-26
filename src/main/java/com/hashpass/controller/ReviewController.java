package com.hashpass.controller;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hashpass.model.Review;
import com.hashpass.model.User;
import com.hashpass.repository.ReviewRepository;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserService;

@Controller
public class ReviewController {

    private static final DateTimeFormatter REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserService userService;
    private final ImageService imageService;
    private final ReviewRepository reviewRepository;

    public ReviewController(UserService userService, ImageService imageService, ReviewRepository reviewRepository) {
        this.userService = userService;
        this.imageService = imageService;
        this.reviewRepository = reviewRepository;
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

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("reviewsList", buildReviewsView());
        prepareReviewForm(model);
        return "reviews";
    }

    @PostMapping("/reviews")
    public String createReview(@RequestParam String title,
            @RequestParam String comment,
            @RequestParam Integer rating,
            RedirectAttributes redirectAttributes) {
        if (!userService.getLoggedUser().isPresent()) {
            return "redirect:/login?redirectTo=/reviews";
        }

        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedComment = comment == null ? "" : comment.trim();

        if (normalizedTitle.isBlank() || normalizedComment.isBlank()) {
            return redirectWithFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    "Debes completar el título y el comentario.");
        }

        if (normalizedTitle.length() > 120) {
            return redirectWithFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    "El título no puede superar los 120 caracteres.");
        }

        if (normalizedComment.length() > 1000) {
            return redirectWithFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    "El comentario no puede superar los 1000 caracteres.");
        }

        if (rating == null || rating < 1 || rating > 5) {
            return redirectWithFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    "La puntuación debe estar entre 1 y 5.");
        }

        Review review = new Review();
        review.setTitle(normalizedTitle);
        review.setComment(normalizedComment);
        review.setRating(rating);
        review.setUser(userService.getLoggedUser().orElse(null));
        reviewRepository.save(review);

        redirectAttributes.addFlashAttribute("reviewSuccess", "Tu reseña se ha publicado correctamente.");
        return "redirect:/reviews";
    }

    private String redirectWithFormError(RedirectAttributes redirectAttributes,
            String title,
            String comment,
            Integer rating,
            String errorMessage) {
        redirectAttributes.addFlashAttribute("reviewError", errorMessage);
        redirectAttributes.addFlashAttribute("reviewTitle", title);
        redirectAttributes.addFlashAttribute("reviewComment", comment);
        redirectAttributes.addFlashAttribute("reviewRating", rating);
        redirectAttributes.addFlashAttribute("openReviewModal", true);
        return "redirect:/reviews";
    }

    private List<Map<String, Object>> buildReviewsView() {
        return reviewRepository.findAllByOrderByCreatedAtDesc().stream().map(review -> {
            Map<String, Object> mappedReview = new HashMap<>();
            User reviewUser = review.getUser();
            String authorName = buildAuthorName(reviewUser);
            String avatarUrl = imageService.getProfileImageUrl(Optional.ofNullable(reviewUser));

            mappedReview.put("title", review.getTitle());
            mappedReview.put("comment", review.getComment());
            mappedReview.put("authorName", authorName);
            mappedReview.put("avatarUrl", avatarUrl != null ? avatarUrl : buildAvatarFallback(authorName));
            mappedReview.put("createdAt", review.getCreatedAt() == null ? "" : REVIEW_DATE_FORMAT.format(review.getCreatedAt()));
            mappedReview.put("stars", buildStars(review.getRating()));
            return mappedReview;
        }).toList();
    }

    private void prepareReviewForm(Model model) {
        Object title = model.asMap().get("reviewTitle");
        Object comment = model.asMap().get("reviewComment");
        Object rating = model.asMap().get("reviewRating");

        model.addAttribute("reviewTitle", title == null ? "" : title);
        model.addAttribute("reviewComment", comment == null ? "" : comment);
        model.addAttribute("ratingOptions", buildRatingOptions(parseRating(rating)));
    }

    private Integer parseRating(Object rating) {
        if (rating instanceof Integer integerRating) {
            return integerRating;
        }
        if (rating instanceof String stringRating && !stringRating.isBlank()) {
            try {
                return Integer.parseInt(stringRating);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private List<Map<String, Object>> buildRatingOptions(Integer selectedRating) {
        int effectiveRating = selectedRating == null ? 5 : selectedRating;
        List<Map<String, Object>> options = new ArrayList<>();
        for (int value = 1; value <= 5; value++) {
            Map<String, Object> option = new HashMap<>();
            option.put("value", value);
            option.put("selected", value == effectiveRating);
            options.add(option);
        }
        return options;
    }

    private List<Map<String, Object>> buildStars(Integer rating) {
        int safeRating = rating == null ? 0 : Math.max(0, Math.min(5, rating));
        List<Map<String, Object>> stars = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            Map<String, Object> star = new HashMap<>();
            star.put("filled", index <= safeRating);
            stars.add(star);
        }
        return stars;
    }

    private String buildAuthorName(User reviewUser) {
        if (reviewUser == null) {
            return "Usuario de HashPass";
        }
        if (reviewUser.getName() != null && !reviewUser.getName().isBlank()) {
            return reviewUser.getName();
        }
        if (reviewUser.getEmail() != null && !reviewUser.getEmail().isBlank()) {
            return reviewUser.getEmail();
        }
        return "Usuario de HashPass";
    }

    private String buildAvatarFallback(String authorName) {
        return "https://ui-avatars.com/api/?name="
                + URLEncoder.encode(authorName, StandardCharsets.UTF_8)
                + "&background=random";
    }
}