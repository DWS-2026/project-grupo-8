package com.hashpass.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.hashpass.model.Plan;
import com.hashpass.repository.PlanRepository;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public List<Plan> findAll() {
        return planRepository.findAll();
    }

    public Optional<Plan> findById(Long id) {
        return planRepository.findById(id);
    }

    public Optional<Plan> findByName(String name) {
        return planRepository.findByName(name);
    }

    public Plan save(Plan plan) {
        return planRepository.save(plan);
    }

    public void delete(Plan plan) {
        planRepository.delete(plan);
    }
}