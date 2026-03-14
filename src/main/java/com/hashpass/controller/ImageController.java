package com.hashpass.controller;

import java.util.Optional;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.hashpass.model.Image;
import com.hashpass.model.CredentialImage;
import com.hashpass.service.ImageService;

@Controller
public class ImageController {

	private final ImageService imageService;

	public ImageController(ImageService imageService) {
		this.imageService = imageService;
	}

	@GetMapping("/images/profile/{userId}")
	public ResponseEntity<byte[]> profileImage(@PathVariable Long userId) {
		Optional<Image> imageOptional = imageService.findProfileImage(userId);
		if (imageOptional.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		Image image = imageOptional.get();
		MediaType mediaType;
		try {
			mediaType = MediaType.parseMediaType(image.getContentType());
		} catch (Exception e) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue())
				.body(image.getData());
	}

	@GetMapping("/images/credential/{credentialId}")
	public ResponseEntity<byte[]> credentialImage(@PathVariable Long credentialId) {
		Optional<CredentialImage> imageOptional = imageService.findCredentialImage(credentialId);
		if (imageOptional.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		CredentialImage image = imageOptional.get();
		MediaType mediaType;
		try {
			mediaType = MediaType.parseMediaType(image.getContentType());
		} catch (Exception e) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}

		return ResponseEntity.ok()
				.contentType(mediaType)
				.header(HttpHeaders.CACHE_CONTROL, CacheControl.noCache().getHeaderValue())
				.body(image.getData());
	}
}