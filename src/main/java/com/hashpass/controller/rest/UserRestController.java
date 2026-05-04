package com.hashpass.controller.rest;

import java.util.Map;

import com.hashpass.model.User;
import com.hashpass.security.HtmlSanitizer;
import com.hashpass.service.AuthService;
import com.hashpass.service.UserService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/v1/users")
public class UserRestController {

    private final AuthService authService;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final HtmlSanitizer htmlSanitizer;

    public UserRestController(AuthService authService, UserService userService,
            AuthenticationManager authenticationManager, HtmlSanitizer htmlSanitizer) {
        this.authService = authService;
        this.userService = userService;
        this.authenticationManager = authenticationManager;
        this.htmlSanitizer = htmlSanitizer;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Los campos email y password son obligatorios."));
        }

        String email = request.email().trim().toLowerCase();
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));

            authService.loginSuccess(email);
            return ResponseEntity.ok(Map.of("message", "Login successful."));
        } catch (AuthenticationException ex) {
            authService.loginFailed(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid credentials."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody CreateUserRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.password2() == null || request.password2().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Los campos name, email, password y confirm-password son obligatorios."));
        }

        try {
            User createdUser = authService.registerUser(
                    request.name().trim(),
                    request.email().trim(),
                    request.phone(),
                    request.password(),
                    request.password2(),
                    request.planId(),
                    false);

            var location = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/api/v1/users/{id}")
                    .buildAndExpand(createdUser.getId())
                    .toUri();

            return ResponseEntity.created(location).body(toResponse(createdUser));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers(Pageable pageable) {
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para acceder a este recurso."));
        }

        var loggedUser = loggedUserOpt.get();
        // Only admins can list all users (prevents user enumeration)
        if (!loggedUser.isAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permisos para acceder a este recurso."));
        }

        return ResponseEntity.ok(userService.findAllUsers(pageable).map(this::toResponse));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para acceder a este recurso."));
        }

        return ResponseEntity.ok(toResponse(loggedUserOpt.get()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para acceder a este recurso."));
        }

        var loggedUser = loggedUserOpt.get();
        if (!loggedUser.isAdmin() && !loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permisos para acceder a este recurso."));
        }

        return userService.findById(id)
                .map(user -> ResponseEntity.ok(toResponse(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody CreateUserRequest request) {
        if (request.name() == null || request.name().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null || request.password().isBlank()
                || request.password2() == null || request.password2().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", "Los campos name, email, password y confirm-password son obligatorios."));
        }

        try {
            User createdUser = authService.registerUser(
                    request.name().trim(),
                    request.email().trim(),
                    request.phone(),
                    request.password(),
                    request.password2(),
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
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para acceder a este recurso."));
        }

        var loggedUser = loggedUserOpt.get();
        if (!loggedUser.isAdmin() && !loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permisos para acceder a este recurso."));
        }

        User user = userService.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        if (request.name() != null && !request.name().isBlank()) {
            String sanitizedName = htmlSanitizer.sanitizePlainText(request.name());
            if (sanitizedName == null || sanitizedName.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("message", "El nombre no puede quedar vacío."));
            }
            if (sanitizedName.length() > 120) {
                return ResponseEntity.badRequest().body(Map.of("message", "El nombre no puede superar 120 caracteres."));
            }
            user.setName(sanitizedName);
        }

        if (request.email() != null && !request.email().isBlank()) {
            String normalizedEmail = htmlSanitizer.normalizeEmail(request.email());
            boolean emailInUse = userService.findByEmail(normalizedEmail)
                    .map(found -> !found.getId().equals(user.getId()))
                    .orElse(false);
            if (emailInUse) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("message", "El correo ya está registrado."));
            }
            user.setEmail(normalizedEmail);
        }

        if (request.phone() != null) {
            String sanitizedPhone = htmlSanitizer.sanitizePhoneNumber(request.phone());
            if (request.phone().isBlank()) {
                user.setPhone(null);
            } else if (sanitizedPhone == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "El teléfono no es válido."));
            } else {
                user.setPhone(sanitizedPhone);
            }
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
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var loggedUser = loggedUserOpt.get();
        if (!loggedUser.isAdmin() && !loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!userService.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Upload a document for a user.
     * POST /api/v1/users/{id}/document
     */
    @PostMapping("/{id}/document")
    public ResponseEntity<?> uploadUserDocument(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para acceder a este recurso."));
        }

        var loggedUser = loggedUserOpt.get();
        if (!loggedUser.isAdmin() && !loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No puedes subir documentos para otros usuarios."));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "El archivo está vacío."));
        }

        try {
            User updatedUser = userService.uploadUserDocument(id, file);
            return ResponseEntity.ok(Map.of(
                    "message", "Documento subido exitosamente.",
                    "originalFilename", userService.getUserDocumentOriginalFilename(id),
                    "user", toResponse(updatedUser)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al subir el documento: " + e.getMessage()));
        }
    }

    /**
     * Download a document for a user.
     * GET /api/v1/users/{id}/document
     */
    @GetMapping("/{id}/document")
    public ResponseEntity<?> downloadUserDocument(@PathVariable Long id) {
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var loggedUser = loggedUserOpt.get();
        if (!loggedUser.isAdmin() && !loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            if (!userService.userHasDocument(id)) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = userService.getUserDocument(id);
            String originalFilename = userService.getUserDocumentOriginalFilename(id);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFilename + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(fileContent);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete a document for a user.
     * DELETE /api/v1/users/{id}/document
     */
    @DeleteMapping("/{id}/document")
    public ResponseEntity<Void> deleteUserDocument(@PathVariable Long id) {
        var loggedUserOpt = userService.getLoggedUser();
        if (loggedUserOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var loggedUser = loggedUserOpt.get();
        if (!loggedUser.isAdmin() && !loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            userService.deleteUserDocument(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.isAdmin(),
                user.getPlan() != null ? user.getPlan().getId() : null,
                user.getPlan() != null ? user.getPlan().getName() : "Gratuito",
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getSecurityTimeoutMinutes(),
                user.getLastLogin(),
                user.getFailedAttempts());
    }

    public record CreateUserRequest(String name, String email, String phone, String password, String password2, Long planId) {
    }

    public record LoginRequest(String email, String password) {
    }

    public record UpdateUserRequest(String name, String email, String phone, Long planId, Boolean admin, Integer securityTimeoutMinutes) {
    }

    public record UserResponse(Long id,
            String name,
            String email,
            String phone,
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
