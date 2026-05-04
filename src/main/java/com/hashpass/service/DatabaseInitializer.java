package com.hashpass.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.hashpass.model.Credential;
import com.hashpass.model.Plan;
import com.hashpass.model.Review;
import com.hashpass.model.User;
import com.hashpass.security.HtmlSanitizer;
import com.hashpass.repository.CredentialRepository;
import com.hashpass.repository.PlanRepository;
import com.hashpass.repository.ReviewRepository;
import com.hashpass.repository.UserRepository;

/**
 * Service responsible for initializing the database with default data.
 * It runs after the application is fully compiled and ready.
 */
@Service
public class DatabaseInitializer {
	private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

	// =====================================================
	// DISCOUNT CODE CONSTANTS
	// =====================================================
	public static final String DEFAULT_DISCOUNT_CODE = "HASHPASS10";
	public static final BigDecimal DISCOUNT_RATE = new BigDecimal("0.10");
	public static final String DISCOUNT_FAILS_SESSION_KEY = "discountCodeFailedAttempts";

	private final UserRepository userRepository;
	private final CredentialRepository credentialRepository;
	private final ReviewRepository reviewRepository;
	private final PlanRepository planRepository;
	private final PasswordEncoder passwordEncoder;
	private final HtmlSanitizer htmlSanitizer;

	public DatabaseInitializer(UserRepository userRepository, CredentialRepository credentialRepository,
			ReviewRepository reviewRepository, PlanRepository planRepository, PasswordEncoder passwordEncoder,
			HtmlSanitizer htmlSanitizer) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
		this.reviewRepository = reviewRepository;
		this.planRepository = planRepository;
		this.passwordEncoder = passwordEncoder;
		this.htmlSanitizer = htmlSanitizer;
	}

	/**
	 * Initializes the database with default data.
	 * - Creates plans if they do not exist
	 * - Creates admin user if missing
	 * - Creates demo users with credentials and reviews
	 */
	public void initializeDatabase() {
		log.info("Inicializando base de datos...");

		// Initialize plans if they do not exist
		createPlanIfNotExists("Gratuito", BigDecimal.ZERO, "- Hasta 10 credenciales\n- Sin soporte técnico\n- Almacenamiento 100MB\n- Contraseña maestra estándar");
		createPlanIfNotExists("Premium", new BigDecimal("4.99"), "- Credenciales ilimitadas\n- Soporte prioritario\n- Almacenamiento 1GB\n- Seguridad avanzada 24 horas\n- Autenticación de dos factores");
		createPlanIfNotExists("Platinum", new BigDecimal("9.99"), "- Credenciales ilimitadas\n- Soporte 24/7 dedicado\n- Almacenamiento 10GB\n- Cifrado de nivel militar\n- Auditoría completa\n- Sincronización en tiempo real\n- Recuperación ante desastres");

		Plan freePlan = planRepository.findByName("Gratuito").orElse(null);

		// Create admin user
		createAdminUserIfNotExists();

		// Create demo users
		seedDemoUser("Usuario Demo Uno", "demo1@hashpass.local", "+34 600 111 111", "Demo123!", new String[][] {
				{ "Gmail", "https://mail.google.com", "demo1@gmail.com", "Correo principal" },
				{ "Netflix", "https://www.netflix.com", "demo1.netflix", "Streaming familiar" }
		}, new String[][] {
				{ "Recomendada", "La uso desde hace meses y funciona genial.", "4" }
		}, freePlan);

		seedDemoUser("Usuario Demo Dos", "demo2@hashpass.local", "+34 600 222 222", "Demo123!", new String[][] {
				{ "GitHub", "https://github.com", "demo2dev", "Cuenta de desarrollo" },
				{ "Banco", "https://www.bbva.es", "demo2.banco", "Acceso banca online" }
		}, new String[][] {
				{ "Muy util", "Me ayuda a no repetir contrasenas y tener todo ordenado.", "5" },
		}, freePlan);

		log.info("Base de datos inicializada correctamente.");
	}

	/**
	 * Creates an administrator user if it does not exist.
	 */
	private void createAdminUserIfNotExists() {
		String adminEmail = "adminhashpass@gmail.com";
		if (userRepository.findByEmail(adminEmail).isEmpty()) {
			User admin = new User();
			admin.setName(htmlSanitizer.sanitizePlainText("Admin"));
			admin.setEmail(adminEmail);
			admin.setPhone("+34 600 000 000");
			admin.setPasswordHash(passwordEncoder.encode("admin123"));
			admin.setEncryptionKey(deriveKey("admin123"));
			admin.setAdmin(true);
			userRepository.save(admin);
			log.info("Usuario administrador creado.");
		}
	}

	/**
	 * Seeds a demo user with credentials and reviews.
	 * 
	 * @param name User name
	 * @param email User email
	 * @param rawPassword Plain-text password
	 * @param credentialsData Array of [siteName, siteUrl, username, note]
	 * @param reviewsData Array of [title, comment, rating]
	 * @param freePlan Plan to assign to the user
	 */
	private void seedDemoUser(String name, String email, String phone, String rawPassword, String[][] credentialsData,
			String[][] reviewsData, Plan freePlan) {
		User user = userRepository.findByEmail(email).orElseGet(() -> {
			User newUser = new User();
			newUser.setName(htmlSanitizer.sanitizePlainText(name));
			newUser.setEmail(email);
			newUser.setPhone(htmlSanitizer.sanitizePhoneNumber(phone));
			newUser.setPasswordHash(passwordEncoder.encode(rawPassword));
			newUser.setEncryptionKey(deriveKey(rawPassword));
			newUser.setAdmin(false);
			newUser.setSecurityTimeoutMinutes(10);
			if (freePlan != null) {
				newUser.setPlan(freePlan);
			}
			return userRepository.save(newUser);
		});

		// Create demo credentials
		seedDemoCredentials(user, rawPassword, credentialsData);

		// Create demo reviews
		seedDemoReviews(user, reviewsData);

		log.info("Usuario demo creado con datos de ejemplo. name={}", name);
	}

	/**
	 * Seeds demo credentials for a user.
	 * 
	 * @param user Credential owner
	 * @param rawPassword User master password
	 * @param credentialsData Array of [siteName, siteUrl, username, note]
	 */
	private void seedDemoCredentials(User user, String rawPassword, String[][] credentialsData) {
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
			credential.setSiteName(htmlSanitizer.sanitizePlainText(siteName));
			credential.setSiteUrl(htmlSanitizer.sanitizeUrl(data[1]));
			credential.setUsername(htmlSanitizer.sanitizePlainText(data[2]));
			credential.setNote(htmlSanitizer.sanitizeOptionalPlainText(data[3]));
			credential.setPasswordEncrypted(encryptForUser(siteName + "@2026", encryptionKey));
			credentialRepository.save(credential);
		}
	}

	/**
	 * Seeds demo reviews for a user.
	 * 
	 * @param user Review owner
	 * @param reviewsData Array of [title, comment, rating]
	 */
	private void seedDemoReviews(User user, String[][] reviewsData) {
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
			review.setTitle(htmlSanitizer.sanitizePlainText(title));
			review.setComment(htmlSanitizer.sanitizeRichText(data[1]));
			review.setRating(Integer.parseInt(data[2]));
			reviewRepository.save(review);
		}
	}

	/**
	 * Creates a plan if it does not exist.
	 * 
	 * @param name Plan name
	 * @param price Monthly price
	 * @param description Plan description
	 */
	private void createPlanIfNotExists(String name, BigDecimal price, String description) {
		if (planRepository.findByName(name).isEmpty()) {
			Plan plan = new Plan();
			plan.setName(htmlSanitizer.sanitizePlainText(name));
			plan.setPriceMonthly(price);
			plan.setDescription(htmlSanitizer.sanitizeOptionalPlainText(description));
			planRepository.save(plan);
			log.info("Plan creado exitosamente. name={}", name);
		}
	}

	/**
	 * Derives a SHA-256 encryption key from a master password.
	 * 
	 * @param password Master password
	 * @return 32-character encryption key
	 */
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

	/**
	 * Encrypts a demo password using AES.
	 * 
	 * @param raw Plain password
	 * @param userKey User encryption key
	 * @return Base64-encoded encrypted password
	 */
	private String encryptForUser(String raw, String userKey) {
		try {
			// 1. Generate a random IV
			byte[] iv = new byte[16];
			SecureRandom random = new SecureRandom();
			random.nextBytes(iv);
			IvParameterSpec ivSpec = new IvParameterSpec(iv);

			// 2. Encrypt using AES/CBC
			SecretKeySpec key = new SecretKeySpec(userKey.getBytes(StandardCharsets.UTF_8), "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
			byte[] encryptedBytes = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));

			// 3. Prepend IV to encrypted data and encode as Base64
			byte[] ivAndEncrypted = new byte[iv.length + encryptedBytes.length];
			System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
			System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.length, encryptedBytes.length);
			return Base64.getEncoder().encodeToString(ivAndEncrypted);
		} catch (Exception e) {
			throw new RuntimeException("Error cifrando credencial demo", e);
		}
	}
}
