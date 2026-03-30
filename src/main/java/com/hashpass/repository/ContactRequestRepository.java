package com.hashpass.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hashpass.model.ContactRequest;

@Repository
public interface ContactRequestRepository extends JpaRepository<ContactRequest, Long> {
	List<ContactRequest> findAllByOrderByCreatedAtDesc();
}
