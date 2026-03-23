package com.hashpass;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import com.hashpass.model.Credential;
import com.hashpass.model.Plan;
import com.hashpass.model.Review;
import com.hashpass.model.User;
import com.hashpass.repository.CredentialRepository;
import com.hashpass.repository.PlanRepository;
import com.hashpass.repository.ReviewRepository;
import com.hashpass.repository.UserRepository;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner seedData(UserRepository userRepository, CredentialRepository credentialRepository,
			ReviewRepository reviewRepository, PlanRepository planRepository, PasswordEncoder passwordEncoder) {
		return args -> {
			// Inicializar planes si no existen
			createPlanIfNotExists(planRepository, "Gratuito", BigDecimal.ZERO, "Para empezar");
			createPlanIfNotExists(planRepository, "Premium", new BigDecimal("4.99"), "Uso diario avanzado");
			createPlanIfNotExists(planRepository, "Platinum", new BigDecimal("9.99"), "Seguridad profesional");
			Plan freePlan = planRepository.findByName("Gratuito").orElse(null);

			String adminEmail = "adminhashpass@gmail.com";
			if (userRepository.findByEmail(adminEmail).isEmpty()) {
				User admin = new User();
				admin.setName("Admin");
				admin.setEmail(adminEmail);
				admin.setPasswordHash(passwordEncoder.encode("admin123"));
				admin.setAdmin(true);
				userRepository.save(admin);
			}

			seedDemoUser(userRepository, credentialRepository, reviewRepository, passwordEncoder, freePlan,
					"Usuario Demo Uno", "demo1@hashpass.local", "Demo123!", new String[][] {
							{ "Gmail", "https://mail.google.com", "demo1@gmail.com", "Correo principal" },
							{ "Netflix", "https://www.netflix.com", "demo1.netflix", "Streaming familiar" }
					},
					new String[][] {
							{ "Recomendada", "La uso desde hace meses y funciona genial.", "4" }
					});

			seedDemoUser(userRepository, credentialRepository, reviewRepository, passwordEncoder, freePlan,
					"Usuario Demo Dos", "demo2@hashpass.local", "Demo123!", new String[][] {
							{ "GitHub", "https://github.com", "demo2dev", "Cuenta de desarrollo" },
							{ "Banco", "https://www.bbva.es", "demo2.banco", "Acceso banca online" }
					},
					new String[][] {
							{ "Muy util", "Me ayuda a no repetir contrasenas y tener todo ordenado.", "5" },
						
					});
		};
	}

	private void seedDemoUser(UserRepository userRepository, CredentialRepository credentialRepository,
			ReviewRepository reviewRepository, PasswordEncoder passwordEncoder, Plan freePlan, String name, String email,
			String rawPassword, String[][] credentialsData, String[][] reviewsData) {
		User user = userRepository.findByEmail(email).orElseGet(() -> {
			User newUser = new User();
			newUser.setName(name);
			newUser.setEmail(email);
			newUser.setPasswordHash(passwordEncoder.encode(rawPassword));
			newUser.setAdmin(false);
			newUser.setSecurityTimeoutMinutes(10);
			if (freePlan != null) {
				newUser.setPlan(freePlan);
			}
			return userRepository.save(newUser);
		});

		String encryptionKey = deriveKey(rawPassword);
		List<Credential> existingCredentials = credentialRepository.findByUserId(user.getId());
		Set<String> existingSites = new HashSet<>();
		for (Credential credential : existingCredentials) {
			existingSites.add(credential.getSiteName());
		}

		for (String[] data : credentialsData) {
			String siteName = data[0];
			if (existingSites.contains(siteName)) {
				continue;
			}
			Credential credential = new Credential();
			credential.setUser(user);
			credential.setSiteName(siteName);
			credential.setSiteUrl(data[1]);
			credential.setUsername(data[2]);
			credential.setNote(data[3]);
			credential.setPasswordEncrypted(encryptForUser(siteName + "@2026", encryptionKey));
			credentialRepository.save(credential);
		}

		List<Review> existingReviews = reviewRepository.findByUserId(user.getId());
		Set<String> existingReviewTitles = new HashSet<>();
		for (Review review : existingReviews) {
			existingReviewTitles.add(review.getTitle());
		}

		for (String[] data : reviewsData) {
			String title = data[0];
			if (existingReviewTitles.contains(title)) {
				continue;
			}
			Review review = new Review();
			review.setUser(user);
			review.setTitle(title);
			review.setComment(data[1]);
			review.setRating(Integer.parseInt(data[2]));
			reviewRepository.save(review);
		}
	}

	private String deriveKey(String password) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString().substring(0, 32);
		} catch (Exception e) {
			throw new RuntimeException("Error generando la llave de cifrado demo", e);
		}
	}

	private String encryptForUser(String raw, String userKey) {
		try {
			SecretKeySpec key = new SecretKeySpec(userKey.getBytes(StandardCharsets.UTF_8), "AES");
			Cipher cipher = Cipher.getInstance("AES");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			byte[] encryptedBytes = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encryptedBytes);
		} catch (Exception e) {
			throw new RuntimeException("Error cifrando credencial demo", e);
		}
	}

	private void createPlanIfNotExists(PlanRepository planRepository, String name, BigDecimal price, String description) {
		if (planRepository.findByName(name).isEmpty()) {
			Plan plan = new Plan();
			plan.setName(name);
			plan.setPriceMonthly(price);
			plan.setDescription(description);
			planRepository.save(plan);
			System.out.println("✓ Plan '" + name + "' creado exitosamente.");
		}
	}
}