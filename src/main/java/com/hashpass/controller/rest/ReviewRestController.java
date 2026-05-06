package com.hashpass.controller.rest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hashpass.model.Review;
import com.hashpass.model.User;
import com.hashpass.security.HtmlSanitizer;
import com.hashpass.security.RateLimited;
import com.hashpass.service.ReviewService;
import com.hashpass.service.UserService;

@RestController
@RequestMapping("/api/v1/reviews")
@RateLimited(requests = 180, minutes = 1)
public class ReviewRestController {

    private final ReviewService reviewService;
    private final UserService userService;
    private final HtmlSanitizer htmlSanitizer;

    public ReviewRestController(ReviewService reviewService, UserService userService, HtmlSanitizer htmlSanitizer) {
        this.reviewService = reviewService;
        this.userService = userService;
        this.htmlSanitizer = htmlSanitizer;
    }

    @GetMapping
    public Page<ReviewResponse> getAllReviews(Pageable pageable) {
        return reviewService.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @GetMapping("/top")
    public ResponseEntity<List<ReviewResponse>> getTopReviews() {
        List<ReviewResponse> reviews = reviewService.findTop3ByOrderByRatingDescCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReviewById(@PathVariable Long id) {
        return reviewService.findById(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ReviewRequest request) {
        Optional<ResponseEntity<?>> validationError = validateReviewRequest(request);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para crear una reseña."));
        }

        Review review = new Review();
        review.setTitle(htmlSanitizer.sanitizePlainText(request.title()));
        review.setComment(htmlSanitizer.sanitizeRichText(request.comment()));
        review.setRating(request.rating());
        review.setUser(currentUser);

        Review createdReview = reviewService.save(review);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdReview.getId())
                .toUri();

        return ResponseEntity.created(location).body(toResponse(createdReview));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateReview(@PathVariable Long id, @RequestBody ReviewRequest request) {
        Optional<ResponseEntity<?>> validationError = validateReviewRequest(request);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para editar una reseña."));
        }

        Review review = reviewService.findById(id).orElse(null);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canModifyReview(review, currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para editar esta reseña."));
        }

        review.setTitle(htmlSanitizer.sanitizePlainText(request.title()));
        review.setComment(htmlSanitizer.sanitizeRichText(request.comment()));
        review.setRating(request.rating());
        Review updatedReview = reviewService.save(review);
        return ResponseEntity.ok(toResponse(updatedReview));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(@PathVariable Long id) {
        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para eliminar una reseña."));
        }

        Review review = reviewService.findById(id).orElse(null);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        if (!canModifyReview(review, currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para eliminar esta reseña."));
        }

        reviewService.delete(review);
        return ResponseEntity.noContent().build();
    }

    private boolean canModifyReview(Review review, User currentUser) {
        if (review == null || currentUser == null) {
            return false;
        }
        return currentUser.isAdmin() || review.getUser() != null
                && currentUser.getId().equals(review.getUser().getId());
    }

    private Optional<ResponseEntity<?>> validateReviewRequest(ReviewRequest request) {
        if (request == null) {
            return Optional.of(ResponseEntity.badRequest().body(Map.of("message", "El cuerpo de la solicitud es obligatorio.")));
        }

        if (request.title() == null || request.title().isBlank()) {
            return Optional.of(ResponseEntity.badRequest().body(Map.of("message", "El título es obligatorio.")));
        }

        String title = htmlSanitizer.sanitizePlainText(request.title());
        String comment = htmlSanitizer.sanitizeRichText(request.comment());
        String plainTextComment = htmlSanitizer.extractPlainText(comment);
        if (plainTextComment.isBlank()) {
            return Optional.of(ResponseEntity.badRequest().body(Map.of("message", "El comentario es obligatorio.")));
        }
        if (title.length() > 120) {
            return Optional.of(ResponseEntity.badRequest().body(Map.of("message", "El título no puede superar los 120 caracteres.")));
        }

        if (plainTextComment.length() > 1000) {
            return Optional.of(ResponseEntity.badRequest().body(Map.of("message", "El comentario no puede superar los 1000 caracteres.")));
        }

        if (request.rating() == null || request.rating() < 1 || request.rating() > 5) {
            return Optional.of(ResponseEntity.badRequest().body(Map.of("message", "La puntuación debe estar entre 1 y 5.")));
        }

        return Optional.empty();
    }

    private ReviewResponse toResponse(Review review) {
        if (review == null) {
            return null;
        }
        User user = review.getUser();
        return new ReviewResponse(
                review.getId(),
                review.getTitle(),
                htmlSanitizer.sanitizeRichText(review.getComment()),
                review.getRating(),
                user != null ? user.getId() : null,
            user != null ? user.getName() : null,
                review.getCreatedAt());
    }

    public record ReviewRequest(String title, String comment, Integer rating) {
    }

    public record ReviewResponse(Long id,
            String title,
            String comment,
            Integer rating,
            Long userId,
            String userName,
            LocalDateTime createdAt) {
    }
}
