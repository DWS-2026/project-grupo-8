package com.hashpass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hashpass.model.CredentialImage;

public interface CredentialImageRepository extends JpaRepository<CredentialImage, Long> {

	Optional<CredentialImage> findByCredentialId(Long credentialId);

	void deleteByCredentialId(Long credentialId);
}