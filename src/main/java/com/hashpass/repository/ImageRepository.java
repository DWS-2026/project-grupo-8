package com.hashpass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hashpass.model.Image;

public interface ImageRepository extends JpaRepository<Image, Long> {

	Optional<Image> findByUserId(Long userId);

	boolean existsByUserId(Long userId);
}