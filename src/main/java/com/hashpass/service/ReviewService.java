package com.hashpass.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hashpass.model.Review;
import com.hashpass.repository.ReviewRepository;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
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

    public List<Review> findTop3ByOrderByRatingDescCreatedAtDesc() {
        return reviewRepository.findTop3ByOrderByRatingDescCreatedAtDesc();
    }
}