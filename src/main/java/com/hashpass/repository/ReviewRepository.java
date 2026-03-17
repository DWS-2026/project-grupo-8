package com.hashpass.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.hashpass.model.Review;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByUserId(Long userId);

    @EntityGraph(attributePaths = "user")
    List<Review> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "user")
    List<Review> findTop3ByOrderByRatingDescCreatedAtDesc();
}
