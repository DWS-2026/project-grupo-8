package com.hashpass.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hashpass.model.Review;
import com.hashpass.repository.ReviewRepository;
import com.hashpass.security.HtmlSanitizer;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final HtmlSanitizer htmlSanitizer;

    public ReviewService(ReviewRepository reviewRepository, HtmlSanitizer htmlSanitizer) {
        this.reviewRepository = reviewRepository;
        this.htmlSanitizer = htmlSanitizer;
    }

    public Optional<Review> findById(Long id) {
        return reviewRepository.findById(id);
    }

    public Review save(Review review) {
        return reviewRepository.save(review);
    }

    public void delete(Review review) {
        reviewRepository.delete(review);
    }

    public List<Review> findAllByOrderByCreatedAtDesc() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    public Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable) {
        return reviewRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public List<Review> findTop3ByOrderByRatingDescCreatedAtDesc() {
        return reviewRepository.findTop3ByOrderByRatingDescCreatedAtDesc();
    }

    public String sanitizeRichText(String rawHtml) {
        return htmlSanitizer.sanitizeRichText(rawHtml);
    }

    public String extractPlainText(String rawHtml) {
        return htmlSanitizer.extractPlainText(rawHtml);
    }
}