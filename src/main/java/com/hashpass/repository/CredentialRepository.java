package com.hashpass.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hashpass.model.Credential;

public interface CredentialRepository extends JpaRepository<Credential, Long> {

    List<Credential> findByUserId(Long userId);
}
