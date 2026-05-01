package com.hashpass.controller.rest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hashpass.model.Credential;
import com.hashpass.model.User;
import com.hashpass.service.EntryService;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserService;

@RestController
@RequestMapping("/api/v1/credentials")
public class CredentialRestController {

    private final EntryService entryService;
    private final UserService userService;
    private final ImageService imageService;

    public CredentialRestController(EntryService entryService, UserService userService, ImageService imageService) {
        this.entryService = entryService;
        this.userService = userService;
        this.imageService = imageService;
    }

    @GetMapping
    public ResponseEntity<?> getAllCredentials(Pageable pageable) {
        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para ver tus credenciales."));
        }

        try {
            Page<CredentialResponse> responses = entryService.listCurrentUser(pageable).map(this::toResponse);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al obtener credenciales: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCredentialById(@PathVariable Long id) {
        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para ver credenciales."));
        }

        Optional<Credential> credentialOpt = entryService.findById(id);
        if (credentialOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Credential credential = credentialOpt.get();
        if (!canAccessCredential(currentUser, credential)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para ver esta credencial."));
        }

        return ResponseEntity.ok(toResponse(credential));
    }

    @PostMapping
    public ResponseEntity<?> createCredential(@RequestBody CreateCredentialRequest request) {
        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para crear credenciales."));
        }

        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El cuerpo de la solicitud es obligatorio."));
        }

        try {
            Credential credential = new Credential();
            credential.setSiteName(request.siteName());
            credential.setUsername(request.username());
            
            credential.setSiteUrl(request.siteUrl());
            credential.setNote(request.note());

            Credential createdCredential = entryService.save(credential, request.password());
            
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(createdCredential.getId())
                    .toUri();

            return ResponseEntity.created(location).body(toResponse(createdCredential));
        } catch (IllegalStateException e) {
            // Free plan limit reached
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al crear credencial: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCredential(@PathVariable Long id, @RequestBody UpdateCredentialRequest request) {
        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para editar credenciales."));
        }

        Optional<Credential> credentialOpt = entryService.findById(id);
        if (credentialOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Credential credential = credentialOpt.get();
        if (!canAccessCredential(currentUser, credential)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para editar esta credencial."));
        }

        if (request == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "El cuerpo de la solicitud es obligatorio."));
        }

        try {
            if (request.siteName() != null && !request.siteName().isBlank()) {
                credential.setSiteName(request.siteName());
            }

            if (request.username() != null && !request.username().isBlank()) {
                credential.setUsername(request.username());
            }

            if (request.password() != null && !request.password().isBlank()) {
                // Password will be encrypted by the service
                credential = entryService.save(credential, request.password());
            } else {
                // Only update other fields
                credential = entryService.save(credential, 
                    entryService.decrypt(credential.getPasswordEncrypted(), Optional.of(currentUser)));
            }

            if (request.siteUrl() != null) {
                credential.setSiteUrl(request.siteUrl());
            }

            if (request.note() != null) {
                credential.setNote(request.note());
            }

            Credential updatedCredential = entryService.findById(id).orElse(credential);
            return ResponseEntity.ok(toResponse(updatedCredential));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al actualizar credencial: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCredential(@PathVariable Long id) {
        User currentUser = userService.getLoggedUser().orElse(null);
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Debes iniciar sesión para eliminar credenciales."));
        }

        Optional<Credential> credentialOpt = entryService.findById(id);
        if (credentialOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Credential credential = credentialOpt.get();
        if (!canAccessCredential(currentUser, credential)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "No tienes permiso para eliminar esta credencial."));
        }

        try {
            entryService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Error al eliminar credencial: " + e.getMessage()));
        }
    }

    private boolean canAccessCredential(User currentUser, Credential credential) {
        if (currentUser == null || credential == null || credential.getUser() == null) {
            return false;
        }
        return currentUser.isAdmin() || currentUser.getId().equals(credential.getUser().getId());
    }

    private CredentialResponse toResponse(Credential credential) {
        if (credential == null) {
            return null;
        }

        String decryptedPassword = null;
        try {
            decryptedPassword = entryService.decrypt(credential.getPasswordEncrypted(), userService.getLoggedUser());
        } catch (Exception e) {
            decryptedPassword = "***ERROR DESCIFRAR***";
        }

        String imageUrl = imageService.getCredentialImageUrl(credential.getId());

        User user = credential.getUser();
        return new CredentialResponse(
                credential.getId(),
                credential.getSiteName(),
                credential.getSiteUrl(),
                credential.getUsername(),
                decryptedPassword,
                credential.getNote(),
                user != null ? user.getId() : null,
                imageUrl,
                credential.getCreatedAt(),
                credential.getUpdatedAt());
    }

    public record CreateCredentialRequest(String siteName, String siteUrl, String username, String password, String note) {
    }

    public record UpdateCredentialRequest(String siteName, String siteUrl, String username, String password, String note) {
    }

    public record CredentialResponse(
            Long id,
            String siteName,
            String siteUrl,
            String username,
            String password,
            String note,
            Long userId,
            String imageUrl,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }
}