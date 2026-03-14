package com.hashpass.service;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.hashpass.model.Image;
import com.hashpass.model.User;
import com.hashpass.model.Credential;
import com.hashpass.model.CredentialImage;
import com.hashpass.repository.CredentialImageRepository;
import com.hashpass.repository.CredentialRepository;
import com.hashpass.repository.ImageRepository;
import com.hashpass.repository.UserRepository;


@Service
public class ImageService {

	private static final long MAX_IMAGE_BYTES = 2L * 1024L * 1024L;

	private final ImageRepository imageRepository;
	private final UserRepository userRepository;
	private final CredentialRepository credentialRepository;
	private final CredentialImageRepository credentialImageRepository;

	public ImageService(ImageRepository imageRepository,
						UserRepository userRepository,
						CredentialRepository credentialRepository,
						CredentialImageRepository credentialImageRepository) {
		this.imageRepository = imageRepository;
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
		this.credentialImageRepository = credentialImageRepository;
	}

	public String saveProfileImage(User user, MultipartFile file) {
		if (user == null || user.getId() == null) {
			return "Debes iniciar sesión para subir una imagen.";
		}

		if (file == null || file.isEmpty()) {
			return "Selecciona una imagen antes de subirla.";
		}

		if (file.getSize() > MAX_IMAGE_BYTES) {
			return "La imagen supera el tamaño máximo de 2MB.";
		}

		String contentType = file.getContentType();
		String normalizedType = normalizeImageContentType(contentType, file.getOriginalFilename());
		if (normalizedType == null) {
			return "Solo se permiten archivos de imagen.";
		}

		try {
			User persistentUser = userRepository.findById(user.getId()).orElse(null);
			if (persistentUser == null) {
				return "No se ha encontrado el usuario para asociar la imagen.";
			}

			Image image = imageRepository.findByUserId(user.getId()).orElseGet(Image::new);
			image.setUser(persistentUser);
			image.setFilename(file.getOriginalFilename() == null ? "profile-image" : file.getOriginalFilename());
			image.setContentType(normalizedType);
			image.setData(file.getBytes());
			imageRepository.save(image);
			return null;
		} catch (IOException e) {
			return "No se pudo procesar la imagen.";
		} catch (Exception e) {
			return "No se pudo guardar la imagen en servidor.";
		}
	}

	private String normalizeImageContentType(String contentType, String originalFilename) {
		if (contentType != null) {
			String lower = contentType.toLowerCase(Locale.ROOT).trim();
			if (lower.equals("image/jpeg") || lower.equals("image/png") || lower.equals("image/webp") || lower.equals("image/gif")) {
				return lower;
			}
			if (lower.startsWith("image/")) {
				return lower;
			}
		}

		if (originalFilename == null) {
			return null;
		}

		String lowerName = originalFilename.toLowerCase(Locale.ROOT);
		if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if (lowerName.endsWith(".png")) {
			return "image/png";
		}
		if (lowerName.endsWith(".webp")) {
			return "image/webp";
		}
		if (lowerName.endsWith(".gif")) {
			return "image/gif";
		}

		return null;
	}

	public Optional<Image> findProfileImage(Long userId) {
		return imageRepository.findByUserId(userId);
	}

	public Optional<CredentialImage> findCredentialImage(Long credentialId) {
		return credentialImageRepository.findByCredentialId(credentialId);
	}

	public String getProfileImageUrl(User user) {
		if (user == null || user.getId() == null) {
			return null;
		}

		Optional<Image> imageOptional = imageRepository.findByUserId(user.getId());
		if (imageOptional.isEmpty()) {
			return null;
		}

		Image image = imageOptional.get();
		long version = image.getUpdatedAt() == null
				? 0L
				: image.getUpdatedAt().toEpochSecond(ZoneOffset.UTC);
		return "/images/profile/" + user.getId() + "?v=" + version;
	}

	public String saveCredentialImage(Long credentialId, MultipartFile file, User currentUser) {
		if (currentUser == null || currentUser.getId() == null) {
			return "Debes iniciar sesión para subir una imagen.";
		}

		if (credentialId == null) {
			return "No se ha identificado la credencial.";
		}

		if (file == null || file.isEmpty()) {
			return null;
		}

		if (file.getSize() > MAX_IMAGE_BYTES) {
			return "La imagen supera el tamaño máximo de 2MB.";
		}

		String normalizedType = normalizeImageContentType(file.getContentType(), file.getOriginalFilename());
		if (normalizedType == null) {
			return "Solo se permiten archivos de imagen.";
		}

		Credential credential = credentialRepository.findById(credentialId).orElse(null);
		if (credential == null || credential.getUser() == null || !credential.getUser().getId().equals(currentUser.getId())) {
			return "No tienes permisos para modificar esta credencial.";
		}

		try {
			CredentialImage image = credentialImageRepository.findByCredentialId(credentialId).orElseGet(CredentialImage::new);
			image.setCredential(credential);
			image.setFilename(file.getOriginalFilename() == null ? "credential-image" : file.getOriginalFilename());
			image.setContentType(normalizedType);
			image.setData(file.getBytes());
			credentialImageRepository.save(image);
			return null;
		} catch (IOException e) {
			return "No se pudo procesar la imagen.";
		} catch (Exception e) {
			return "No se pudo guardar la imagen en servidor.";
		}
	}

	public String getCredentialImageUrl(Long credentialId) {
		if (credentialId == null) {
			return null;
		}

		Optional<CredentialImage> imageOptional = credentialImageRepository.findByCredentialId(credentialId);
		if (imageOptional.isEmpty()) {
			return null;
		}

		CredentialImage image = imageOptional.get();
		long version = image.getUpdatedAt() == null
				? 0L
				: image.getUpdatedAt().toEpochSecond(ZoneOffset.UTC);
		return "/images/credential/" + credentialId + "?v=" + version;
	}

	public void deleteCredentialImage(Long credentialId) {
		if (credentialId != null) {
			credentialImageRepository.deleteByCredentialId(credentialId);
		}
	}

	

}