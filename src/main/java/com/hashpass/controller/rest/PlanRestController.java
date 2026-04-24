package com.hashpass.controller.rest;

import java.math.BigDecimal;
import java.util.Map;

import com.hashpass.model.Plan;
import com.hashpass.service.PlanService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/plans")
public class PlanRestController {

    private final PlanService planService;

    public PlanRestController(PlanService planService) {
        this.planService = planService;
    }

    @GetMapping
    public Page<PlanResponse> getAllPlans(Pageable pageable) {
        return planService.findAll(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable Long id) {
        return planService.findById(id)
                .map(plan -> ResponseEntity.ok(toResponse(plan)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createPlan(@RequestBody CreatePlanRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "El campo name es obligatorio."));
        }

        if (request.priceMonthly() == null || request.priceMonthly().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "El priceMonthly debe ser mayor a 0."));
        }

        if (planService.findByName(request.name().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "message", "Ya existe un plan con este nombre."));
        }

        try {
            Plan plan = new Plan();
            plan.setName(request.name().trim());
            plan.setPriceMonthly(request.priceMonthly());
            if (request.description() != null && !request.description().isBlank()) {
                plan.setDescription(request.description().trim());
            }

            Plan createdPlan = planService.save(plan);

            var location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdPlan.getId())
                    .toUri();

            return ResponseEntity.created(location).body(toResponse(createdPlan));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear el plan: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePlan(@PathVariable Long id, @RequestBody UpdatePlanRequest request) {
        Plan plan = planService.findById(id).orElse(null);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.name() != null && !request.name().isBlank()) {
            String newName = request.name().trim();
            if (!newName.equals(plan.getName())) {
                if (planService.findByName(newName).isPresent()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(Map.of("message", "Ya existe un plan con este nombre."));
                }
                plan.setName(newName);
            }
        }

        if (request.priceMonthly() != null) {
            if (request.priceMonthly().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "El priceMonthly debe ser mayor a 0."));
            }
            plan.setPriceMonthly(request.priceMonthly());
        }

        if (request.description() != null) {
            plan.setDescription(request.description().isBlank() ? null : request.description().trim());
        }

        Plan updatedPlan = planService.save(plan);
        return ResponseEntity.ok(toResponse(updatedPlan));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable Long id) {
        Plan plan = planService.findById(id).orElse(null);
        if (plan == null) {
            return ResponseEntity.notFound().build();
        }
        planService.delete(plan);
        return ResponseEntity.noContent().build();
    }

    private PlanResponse toResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getName(),
                plan.getPriceMonthly(),
                plan.getDescription(),
                plan.getUsers().size(),
                plan.getCreatedAt(),
                plan.getUpdatedAt());
    }

    public record CreatePlanRequest(String name, BigDecimal priceMonthly, String description) {
    }

    public record UpdatePlanRequest(String name, BigDecimal priceMonthly, String description) {
    }

    public record PlanResponse(Long id,
            String name,
            BigDecimal priceMonthly,
            String description,
            Integer totalUsers,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt) {
    }

}