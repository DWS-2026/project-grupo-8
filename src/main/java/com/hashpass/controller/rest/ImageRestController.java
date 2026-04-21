package com.hashpass.controller.rest;

import java.util.Map;
import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.hashpass.model.Credential;
import com.hashpass.model.CredentialImage;
import com.hashpass.model.Image;
import com.hashpass.model.User;
import com.hashpass.service.EntryService;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserService;

@RestController
@RequestMapping("/api/v1/images")
public class ImageRestController {

	private final ImageService imageService;
	private final UserService userService;
	private final EntryService entryService;

	public ImageRestController(ImageService imageService,
			UserService userService,
			EntryService entryService) {
		this.imageService = imageService;
		this.userService = userService;
		this.entryService = entryService;
	}

	@GetMapping("/profile/{userId}")
	public ResponseEntity<byte[]> getProfileImage(@PathVariable Long userId) {
		User currentUser = userService.getLoggedUser().orElse(null);
		if (!canAccessUserResource(currentUser, userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Optional<Image> imageOptional = imageService.findProfileImage(userId);
		if (imageOptional.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Image image = imageOptional.get();
		return buildImageResponse(image.getData(), image.getContentType());
	}

	@PutMapping("/profile/{userId}")
	public ResponseEntity<?> uploadProfileImage(@PathVariable Long userId,
			@RequestParam("file") MultipartFile file) {
		User currentUser = userService.getLoggedUser().orElse(null);
		if (!canAccessUserResource(currentUser, userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		User targetUser = userService.findById(userId).orElse(null);
		if (targetUser == null) {
			return ResponseEntity.notFound().build();
		}

		boolean existedBefore = imageService.findProfileImage(userId).isPresent();
		String error = imageService.saveProfileImage(targetUser, file);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}

		var location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
		String url = imageService.getProfileImageUrl(Optional.of(targetUser));
		return existedBefore
				? ResponseEntity.ok(Map.of(
						"message", "Imagen de perfil actualizada correctamente.",
						"url", url))
				: ResponseEntity.created(location).body(Map.of(
						"message", "Imagen de perfil creada correctamente.",
						"url", url));
	}

	@DeleteMapping("/profile/{userId}")
	public ResponseEntity<Void> deleteProfileImage(@PathVariable Long userId) {
		User currentUser = userService.getLoggedUser().orElse(null);
		if (!canAccessUserResource(currentUser, userId)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		if (imageService.findProfileImage(userId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		imageService.deleteProfileImage(userId);
		return ResponseEntity.noContent().build();
	}

	@GetMapping("/credential/{credentialId}")
	public ResponseEntity<byte[]> getCredentialImage(@PathVariable Long credentialId) {
		Credential credential = entryService.findById(credentialId).orElse(null);
		if (credential == null) {
			return ResponseEntity.notFound().build();
		}

		User currentUser = userService.getLoggedUser().orElse(null);
		if (!canAccessCredentialResource(currentUser, credential)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		Optional<CredentialImage> imageOptional = imageService.findCredentialImage(credentialId);
		if (imageOptional.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		CredentialImage image = imageOptional.get();
		return buildImageResponse(image.getData(), image.getContentType());
	}

	@PutMapping("/credential/{credentialId}")
	public ResponseEntity<?> uploadCredentialImage(@PathVariable Long credentialId,
			@RequestParam("file") MultipartFile file) {
		Credential credential = entryService.findById(credentialId).orElse(null);
		if (credential == null) {
			return ResponseEntity.notFound().build();
		}

		User currentUser = userService.getLoggedUser().orElse(null);
		if (!canAccessCredentialResource(currentUser, credential)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		boolean existedBefore = imageService.findCredentialImage(credentialId).isPresent();
		String error = imageService.saveCredentialImage(credentialId, file, currentUser);
		if (error != null) {
			return ResponseEntity.badRequest().body(Map.of("message", error));
		}

		var location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
		String url = imageService.getCredentialImageUrl(credentialId);
		return existedBefore
				? ResponseEntity.ok(Map.of(
						"message", "Imagen de credencial actualizada correctamente.",
						"url", url))
				: ResponseEntity.created(location).body(Map.of(
						"message", "Imagen de credencial creada correctamente.",
						"url", url));
	}

	@DeleteMapping("/credential/{credentialId}")
	public ResponseEntity<Void> deleteCredentialImage(@PathVariable Long credentialId) {
		Credential credential = entryService.findById(credentialId).orElse(null);
		if (credential == null) {
			return ResponseEntity.notFound().build();
		}

		User currentUser = userService.getLoggedUser().orElse(null);
		if (!canAccessCredentialResource(currentUser, credential)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}

		if (imageService.findCredentialImage(credentialId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		imageService.deleteCredentialImage(credentialId);
		return ResponseEntity.noContent().build();
	}

	private ResponseEntity<byte[]> buildImageResponse(byte[] data, String contentType) {
		MediaType mediaType;
		try {
			mediaType = MediaType.parseMediaType(contentType);
		} catch (Exception e) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue())
				.body(data);
	}

	private boolean canAccessUserResource(User currentUser, Long userId) {
		if (currentUser == null || userId == null) {
			return false;
		}
		return currentUser.isAdmin() || userId.equals(currentUser.getId());
	}

	private boolean canAccessCredentialResource(User currentUser, Credential credential) {
		if (currentUser == null || credential == null || credential.getUser() == null) {
			return false;
		}
		return currentUser.isAdmin() || currentUser.getId().equals(credential.getUser().getId());
	}
}
