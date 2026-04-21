package com.hashpass.controller.rest;

import java.util.Map;

import com.hashpass.model.User;
import com.hashpass.service.AuthService;
import com.hashpass.service.UserService;

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
@RequestMapping("/api/v1/users")
public class UserRestController {

    private final AuthService authService;
    private final UserService userService;

    public UserRestController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    @GetMapping
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userService.findAllUsers(pageable).map(this::toResponse);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return userService.findById(id)
                .map(user -> ResponseEntity.ok(toResponse(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Los campos name, email y password son obligatorios."));
        }

        try {
            User createdUser = authService.registerUser(
                    request.name().trim(),
                    request.email().trim(),
                    request.password(),
                    request.planId(),
                    true);

            var location = ServletUriComponentsBuilder
                    .fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdUser.getId())
                    .toUri();

            return ResponseEntity.created(location).body(toResponse(createdUser));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody UpdateUserRequest request) {
        User user = userService.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = request.email().trim().toLowerCase();
            boolean emailInUse = userService.findByEmail(normalizedEmail)
                    .map(found -> !found.getId().equals(user.getId()))
                    .orElse(false);
            if (emailInUse) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "El correo ya está registrado."));
            }
            user.setEmail(normalizedEmail);
        }

        if (request.planId() != null) {
            if (request.planId() <= 0) {
                user.setPlan(null);
            } else {
                var plan = userService.findPlanById(request.planId()).orElse(null);
                if (plan == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "El plan indicado no existe."));
                }
                user.setPlan(plan);
            }
        }

        if (request.admin() != null) {
            user.setAdmin(request.admin());
        }

        if (request.securityTimeoutMinutes() != null) {
            if (request.securityTimeoutMinutes() < 1 || request.securityTimeoutMinutes() > 120) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "securityTimeoutMinutes debe estar entre 1 y 120."));
            }
            user.setSecurityTimeoutMinutes(request.securityTimeoutMinutes());
        }

        User updatedUser = userService.setUser(user);
        return ResponseEntity.ok(toResponse(updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.isAdmin(),
                user.getPlan() != null ? user.getPlan().getId() : null,
                user.getPlan() != null ? user.getPlan().getName() : "Gratuito",
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getSecurityTimeoutMinutes(),
                user.getLastLogin(),
                user.getFailedAttempts());
    }

    public record CreateUserRequest(String name, String email, String password, Long planId) {
    }

    public record UpdateUserRequest(String name, String email, Long planId, Boolean admin, Integer securityTimeoutMinutes) {
    }

    public record UserResponse(Long id,
            String name,
            String email,
            boolean admin,
            Long planId,
            String planName,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt,
            Integer securityTimeoutMinutes,
            java.time.LocalDateTime lastLogin,
            Integer failedAttempts) {
    }

}
