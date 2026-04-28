package com.hashpass.controller.web;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.hashpass.service.ImageService;
import com.hashpass.service.ReviewService;
import com.hashpass.service.UserService;

@Controller
public class ReviewController {

    private static final DateTimeFormatter REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserService userService;
    private final ImageService imageService;
    private final ReviewService reviewService;

    public ReviewController(UserService userService, ImageService imageService, ReviewService reviewService) {
        this.userService = userService;
        this.imageService = imageService;
        this.reviewService = reviewService;
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
    public String reviews(Model model, @RequestParam(name = "editId", required = false) Long editId) {
        model.addAttribute("reviewsList", buildReviewsView());

        if (editId != null && !model.containsAttribute("reviewId")) {
            Optional<User> logged = userService.getLoggedUser();
            Optional<Review> reviewOpt = reviewService.findById(editId);
            if (reviewOpt.isPresent() && logged.isPresent()) {
                Review review = reviewOpt.get();
                if (isAllowedToModifyReview(review, logged.get())) {
                    model.addAttribute("editMode", true);
                    model.addAttribute("reviewId", review.getId());
                    model.addAttribute("reviewTitle", review.getTitle());
                    model.addAttribute("reviewComment", review.getComment());
                    model.addAttribute("reviewRating", review.getRating());
                    model.addAttribute("openReviewModal", true);
                } else {
                    model.addAttribute("reviewError", "No tienes permiso para editar esta reseña.");
                }
            } else {
                model.addAttribute("reviewError", "Reseña no encontrada.");
            }
        }

        prepareReviewForm(model);
        return "reviews";
    }

    @PostMapping("/reviews")
    public String createReview(@RequestParam String title,
            @RequestParam String comment,
            @RequestParam Integer rating,
            RedirectAttributes redirectAttributes) {
        Optional<User> logged = userService.getLoggedUser();
        if (logged.isEmpty()) {
            return "redirect:/login?redirectTo=/reviews";
        }

        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedComment = reviewService.sanitizeRichText(comment);
        String plainTextComment = reviewService.extractPlainText(normalizedComment);

        if (normalizedTitle.isBlank() || plainTextComment.isBlank()) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    false, "Debes completar el título y el comentario.");
        }

        if (normalizedTitle.length() > 120) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    false, "El título no puede superar los 120 caracteres.");
        }

        if (plainTextComment.length() > 1000) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    false, "El comentario no puede superar los 1000 caracteres.");
        }

        if (rating == null || rating < 1 || rating > 5) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    false, "La puntuación debe estar entre 1 y 5.");
        }

        Review review = new Review();
        review.setTitle(normalizedTitle);
        review.setComment(normalizedComment);
        review.setRating(rating);
        review.setUser(logged.get());
        reviewService.save(review);

        redirectAttributes.addFlashAttribute("reviewSuccess", "Tu reseña se ha publicado correctamente.");
        return "redirect:/reviews";
    }

    @PostMapping("/reviews/edit")
    public String editReview(@RequestParam Long id,
            @RequestParam String title,
            @RequestParam String comment,
            @RequestParam Integer rating,
            RedirectAttributes redirectAttributes) {
        Optional<User> logged = userService.getLoggedUser();
        if (logged.isEmpty()) {
            return "redirect:/login?redirectTo=/reviews";
        }

        Optional<Review> reviewOpt = reviewService.findById(id);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("reviewError", "Reseña no encontrada.");
            return "redirect:/reviews";
        }

        Review review = reviewOpt.get();
        if (!isAllowedToModifyReview(review, logged.get())) {
            redirectAttributes.addFlashAttribute("reviewError", "No tienes permiso para editar esta reseña.");
            return "redirect:/reviews";
        }

        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedComment = reviewService.sanitizeRichText(comment);
        String plainTextComment = reviewService.extractPlainText(normalizedComment);

        if (normalizedTitle.isBlank() || plainTextComment.isBlank()) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    true, "Debes completar el título y el comentario.", id);
        }

        if (normalizedTitle.length() > 120) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    true, "El título no puede superar los 120 caracteres.", id);
        }

        if (plainTextComment.length() > 1000) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    true, "El comentario no puede superar los 1000 caracteres.", id);
        }

        if (rating == null || rating < 1 || rating > 5) {
            return redirectWithReviewFormError(redirectAttributes, normalizedTitle, normalizedComment, rating,
                    true, "La puntuación debe estar entre 1 y 5.", id);
        }

        review.setTitle(normalizedTitle);
        review.setComment(normalizedComment);
        review.setRating(rating);
        reviewService.save(review);

        redirectAttributes.addFlashAttribute("reviewSuccess", "Tu reseña se ha actualizado correctamente.");
        return "redirect:/reviews";
    }

    @PostMapping("/reviews/delete")
    public String deleteReview(@RequestParam Long id, RedirectAttributes redirectAttributes) {
        Optional<User> logged = userService.getLoggedUser();
        if (logged.isEmpty()) {
            return "redirect:/login?redirectTo=/reviews";
        }

        Optional<Review> reviewOpt = reviewService.findById(id);
        if (reviewOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("reviewError", "Reseña no encontrada.");
            return "redirect:/reviews";
        }

        Review review = reviewOpt.get();
        if (!isAllowedToDeleteReview(review, logged.get())) {
            redirectAttributes.addFlashAttribute("reviewError", "No tienes permiso para borrar esta reseña.");
            return "redirect:/reviews";
        }

        reviewService.delete(review);
        redirectAttributes.addFlashAttribute("reviewSuccess", "Reseña eliminada correctamente.");
        return "redirect:/reviews";
    }

    private String redirectWithReviewFormError(RedirectAttributes redirectAttributes,
            String title,
            String comment,
            Integer rating,
            boolean editMode,
            String errorMessage) {
        redirectAttributes.addFlashAttribute("reviewError", errorMessage);
        redirectAttributes.addFlashAttribute("reviewTitle", title);
        redirectAttributes.addFlashAttribute("reviewComment", comment);
        redirectAttributes.addFlashAttribute("reviewRating", rating);
        if (editMode) {
            redirectAttributes.addFlashAttribute("editMode", true);
        }
        redirectAttributes.addFlashAttribute("openReviewModal", true);
        return "redirect:/reviews";
    }

    private String redirectWithReviewFormError(RedirectAttributes redirectAttributes,
            String title,
            String comment,
            Integer rating,
            boolean editMode,
            String errorMessage,
            Long id) {
        redirectWithReviewFormError(redirectAttributes, title, comment, rating, editMode, errorMessage);
        redirectAttributes.addFlashAttribute("reviewId", id);
        return "redirect:/reviews";
    }

    private List<Map<String, Object>> buildReviewsView() {
        Optional<User> logged = userService.getLoggedUser();
        return reviewService.findAllByOrderByCreatedAtDesc().stream().map(review -> {
            Map<String, Object> mappedReview = new HashMap<>();
            User reviewUser = review.getUser();
            String authorName = buildAuthorName(reviewUser);
            String avatarUrl = imageService.getProfileImageUrl(Optional.ofNullable(reviewUser));

            boolean isOwner = logged.isPresent() && reviewUser != null && Objects.equals(reviewUser.getId(), logged.get().getId());
            boolean isAdmin = logged.map(User::isAdmin).orElse(false);

            mappedReview.put("id", review.getId());
            mappedReview.put("title", review.getTitle());
            mappedReview.put("comment", reviewService.sanitizeRichText(review.getComment()));
            mappedReview.put("authorName", authorName);
            mappedReview.put("avatarUrl", avatarUrl != null ? avatarUrl : buildAvatarFallback(authorName));
            mappedReview.put("createdAt", review.getCreatedAt() == null ? "" : REVIEW_DATE_FORMAT.format(review.getCreatedAt()));
            mappedReview.put("stars", buildStars(review.getRating()));

            mappedReview.put("canEdit", isOwner);
            mappedReview.put("canDelete", isOwner || isAdmin);
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

        if (model.asMap().containsKey("editMode")) {
            model.addAttribute("editMode", model.asMap().get("editMode"));
        }
        if (model.asMap().containsKey("reviewId")) {
            model.addAttribute("reviewId", model.asMap().get("reviewId"));
        }
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

    private boolean isAllowedToModifyReview(Review review, User logged) {
        if (review == null || logged == null) {
            return false;
        }
        return review.getUser() != null && Objects.equals(review.getUser().getId(), logged.getId());
    }

    private boolean isAllowedToDeleteReview(Review review, User logged) {
        if (review == null || logged == null) {
            return false;
        }
        return isAllowedToModifyReview(review, logged) || logged.isAdmin();
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