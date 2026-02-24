package com.hashpass.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hashpass.model.Plan;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByName(String name);
}
