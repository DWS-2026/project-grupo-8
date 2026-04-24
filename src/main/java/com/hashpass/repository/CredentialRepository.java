package com.hashpass.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.hashpass.model.Credential;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    List<Credential> findByUserId(Long userId);

    Page<Credential> findByUserId(Long userId, Pageable pageable);

    long countByUserId(Long userId);
}
