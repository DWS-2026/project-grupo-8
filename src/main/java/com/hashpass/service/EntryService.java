package com.hashpass.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import javax.crypto.spec.IvParameterSpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.hashpass.model.Credential;
import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.security.HtmlSanitizer;
import com.hashpass.repository.CredentialRepository;

@Service
public class EntryService {

    private static final Logger log = LoggerFactory.getLogger(EntryService.class);

    private static final int FREE_PLAN_CREDENTIAL_LIMIT = 10;
    public static final String FREE_PLAN_LIMIT_MESSAGE =
            "Con el plan gratuito solo puedes guardar hasta 10 credenciales. Mejora tu plan para añadir más.";

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private HtmlSanitizer htmlSanitizer;
    /**
     * obtains the list of credentials of the current logged user. If there is no user in session, throws an exception.
     */
    public List<Credential> listCurrentUser() {
        Optional<User> u = userService.getLoggedUser();
        if (u.isEmpty()) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
        return credentialRepository.findByUserId(u.get().getId());
    }

    public Page<Credential> listCurrentUser(Pageable pageable) {
        Optional<User> u = userService.getLoggedUser();
        if (u.isEmpty()) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
        return credentialRepository.findByUserId(u.get().getId(), pageable);
    }

    /**
     * saves the credential of the current logged user.
     * If there is no user in session, throws an exception.
     * If the credential is new and the user has a free plan with 10 or more credentials, throws an exception.
     * If the credential is null or has empty fields, throws an exception.
     */
    public Credential save(Credential cred, String plainPassword) {
        Optional<User> u = userService.getLoggedUser();
        if (!u.isPresent()) {
            throw new IllegalStateException("Debe iniciar sesión para guardar credenciales");
        }

        if (cred == null) {
            throw new IllegalArgumentException("La credencial no puede ser nula.");
        }

        String normalizedSiteName = htmlSanitizer.sanitizePlainText(cred.getSiteName());
        String normalizedUsername = htmlSanitizer.sanitizePlainText(cred.getUsername());
        String normalizedPassword = plainPassword == null ? "" : plainPassword.trim();
        String normalizedSiteUrl = htmlSanitizer.sanitizeUrl(cred.getSiteUrl());
        String normalizedNote = htmlSanitizer.sanitizeOptionalPlainText(cred.getNote());

        if (normalizedSiteName == null || normalizedSiteName.isBlank()
                || normalizedUsername == null || normalizedUsername.isBlank()
                || normalizedPassword.isBlank()) {
            throw new IllegalArgumentException("Debes completar servicio, usuario y contraseña.");
        }
        if (normalizedSiteName.length() > 120) {
            throw new IllegalArgumentException("El servicio no puede superar 120 caracteres.");
        }
        if (normalizedUsername.length() > 120) {
            throw new IllegalArgumentException("El usuario no puede superar 120 caracteres.");
        }
        if (normalizedNote != null && normalizedNote.length() > 1000) {
            throw new IllegalArgumentException("La nota no puede superar 1000 caracteres.");
        }

        boolean isNewCredential = cred.getId() == null;
        if (isNewCredential && isFreePlan(u)) {
            long currentCredentials = credentialRepository.countByUserId(u.get().getId());
            if (currentCredentials >= FREE_PLAN_CREDENTIAL_LIMIT) {
                throw new IllegalStateException(FREE_PLAN_LIMIT_MESSAGE);
            }
        }

        cred.setSiteName(normalizedSiteName);
        cred.setUsername(normalizedUsername);
        cred.setSiteUrl(normalizedSiteUrl);
        cred.setNote(normalizedNote);
        cred.setUser(u.get());
        cred.setPasswordEncrypted(encrypt(normalizedPassword, u));
        return credentialRepository.save(cred);
    }

    private boolean isFreePlan(Optional<User> userOpt) {
        if (!userOpt.isPresent()) {
            return true;
        }
        User user = userOpt.get();
        Plan plan = user.getPlan();
        if (plan == null || plan.getName() == null) {
            return true;
        }
        String planName = plan.getName().trim();
        return "Gratuito".equalsIgnoreCase(planName) || "Free".equalsIgnoreCase(planName);
    }

    public Optional<Credential> findById(Long id) {
        return credentialRepository.findById(id);
    }

    public void delete(Long id) {
        Optional<Credential> credOpt = credentialRepository.findById(id);
        if (credOpt.isPresent()) {
            credentialRepository.deleteById(id);
            credentialRepository.flush();
        }
    }

    // ------------------------------------------------------------------
    // Helpers {Decrypt/Encrypt, Re-encrypt}
    // ------------------------------------------------------------------

    private String encrypt(String raw, Optional<User> userOpt) {
        if (raw == null) return null;
        if (!userOpt.isPresent()) {
            throw new IllegalStateException("No hay usuario en sesión para cifrar la contraseña");
        }
        User user = userOpt.get();

        // 1. We ask for the master key from the session temporary file, where it has been stored
        String userKey = user.getEncryptionKey();
        if (userKey == null) {
            throw new IllegalStateException("No hay llave de cifrado en la sesión");
        }

        try {
            // 2. Generate a random IV
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 3. We close the AES lock using the user's personal key
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(userKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, ivSpec);
            
            byte[] encryptedBytes = cipher.doFinal(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 4. Prepend IV to encrypted data and encode as Base64
            byte[] ivAndEncrypted = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.length, encryptedBytes.length);
            return java.util.Base64.getEncoder().encodeToString(ivAndEncrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar la contraseña", e);
        }
    }

    public String decrypt(String encrypted, Optional<User> userOpt) {
        if (encrypted == null) return null;
        if (!userOpt.isPresent()) {
            throw new IllegalStateException("No hay usuario en sesión para descifrar la contraseña");
        }
        User user = userOpt.get();

        // 1. We ask for the master key from the session temporary file, where it has been stored
        String userKey = user.getEncryptionKey();
        if (userKey == null) {
            throw new IllegalStateException("No hay llave de descifrado en la sesión");
        }

        try {
            // 2. Decode Base64
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encrypted);

            // 3. Extract IV (first 16 bytes) and encrypted data
            if (decodedBytes.length < 16) {
                throw new IllegalArgumentException("Datos cifrados inválidos");
            }
            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[decodedBytes.length - 16];
            System.arraycopy(decodedBytes, 0, iv, 0, 16);
            System.arraycopy(decodedBytes, 16, encryptedBytes, 0, encryptedBytes.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 4. We close the AES lock using the user's personal key
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(userKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, ivSpec);
            
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar la contraseña. ¿Llave incorrecta?", e);
        }
    }

    // ------------------------------------------------------------------
    // User's mastery key rotation (re-encryption)
    // ------------------------------------------------------------------

    /**
     * Derives an encryption key from a master password.
     * Uses SHA-256 for hashing a 32-character key (256 bits).
     */
    private String deriveEncryptionKey(String masterPassword) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(masterPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8));
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
            throw new RuntimeException("Error derivando la llave de cifrado", e);
        }
    }

    /**
     * Encrypts a password using a provided key (not the session key).
     * We use this during password migration when the session key is not available or has changed.
     */
    private String encryptWithKey(String raw, String encryptionKey) {
        if (raw == null) return null;
        
        try {
            // 1. Generate a random IV
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 2. We close the AES lock using the provided key
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(encryptionKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key, ivSpec);
            
            byte[] encryptedBytes = cipher.doFinal(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // 3. Prepend IV to encrypted data and encode as Base64
            byte[] ivAndEncrypted = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, ivAndEncrypted, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, ivAndEncrypted, iv.length, encryptedBytes.length);
            return java.util.Base64.getEncoder().encodeToString(ivAndEncrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar la contraseña", e);
        }
    }

    /**
     * Encrypts a password using a provided key (not the session key).
     * We use this during password migration when the session key is not available or has changed.
     */
    private String decryptWithKey(String encrypted, String encryptionKey) {
        if (encrypted == null) return null;
        
        try {
            // 1. Decode Base64
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encrypted);

            // 2. Extract IV (first 16 bytes) and encrypted data
            if (decodedBytes.length < 16) {
                throw new IllegalArgumentException("Datos cifrados inválidos");
            }
            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[decodedBytes.length - 16];
            System.arraycopy(decodedBytes, 0, iv, 0, 16);
            System.arraycopy(decodedBytes, 16, encryptedBytes, 0, encryptedBytes.length);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            // 3. We close the AES lock using the provided key
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(encryptionKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key, ivSpec);
            
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar la contraseña. ¿Llave incorrecta?", e);
        }
    }

    /**
     * Re-encrypts all credentials of a user with a new master password.
     * First decrypts with the old key, then encrypts with the new key.
     * 
     * @param userId ID of the user
     * @param oldPassword Previous master password (unhashed)
     * @param newPassword New master password (unhashed)
     * @return Error message if something fails, null if successful
     */
    public String reEncryptAllCredentials(Long userId, String oldPassword, String newPassword) {
        try {
            // Derive the keys for both passwords
            String oldKey = deriveEncryptionKey(oldPassword);
            String newKey = deriveEncryptionKey(newPassword);
            
            // Get all credentials for the user
            List<Credential> credentials = credentialRepository.findByUserId(userId);
            
            // Re-encrypt each credential
            for (Credential cred : credentials) {
                try {
                    // Decrypt with the old key
                    String decrypted = decryptWithKey(cred.getPasswordEncrypted(), oldKey);
                    
                    // Encrypt with the new key
                    String encrypted = encryptWithKey(decrypted, newKey);
                    
                    // Update the credential
                    cred.setPasswordEncrypted(encrypted);
                    credentialRepository.save(cred);
                } catch (Exception e) {
                    // If a credential fails, log the error but continue
                    log.warn("SECURITY_EVENT=CREDENTIAL_REENCRYPT_FAILED credentialId={} userId={} reason={}",
                            cred.getId(), userId, e.getClass().getSimpleName(), e);
                }
            }
            
            return null; // Success
        } catch (Exception e) {
            log.error("SECURITY_EVENT=CREDENTIAL_REENCRYPT_BATCH_FAILED userId={} reason={}",
                    userId, e.getClass().getSimpleName(), e);
            return "Error al re-cifrar las credenciales: " + e.getMessage();
        }
    }
}