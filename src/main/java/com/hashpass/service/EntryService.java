package com.hashpass.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hashpass.model.Credential;
import com.hashpass.model.Plan;
import com.hashpass.model.User;
import com.hashpass.repository.CredentialRepository;
import com.hashpass.service.UserService;

@Service
public class EntryService {

    private static final int FREE_PLAN_CREDENTIAL_LIMIT = 10;
    public static final String FREE_PLAN_LIMIT_MESSAGE =
            "Con el plan gratuito solo puedes guardar hasta 10 credenciales. Mejora tu plan para añadir más.";

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private UserService userService;
    /**
     * Obtiene todas las credenciales pertenecientes al usuario actualmente
     * almacenado en la sesión. Si no hay usuario lanza IllegalStateException.
     */
    public List<Credential> listCurrentUser() {
        Optional<User> u = userService.getLoggedUser();
        if (u.isEmpty()) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
        return credentialRepository.findByUserId(u.get().getId());
    }

    /**
     * Guarda una nueva credencial o actualiza una existente. El parámetro
     * plainPassword es cifrado/obfuscado antes de persistir.
     */
    public Credential save(Credential cred, String plainPassword) {
        Optional<User> u = userService.getLoggedUser();
        if (!u.isPresent()) {
            throw new IllegalStateException("Debe iniciar sesión para guardar credenciales");
        }

        if (cred == null) {
            throw new IllegalArgumentException("La credencial no puede ser nula.");
        }

        String normalizedSiteName = cred.getSiteName() == null ? "" : cred.getSiteName().trim();
        String normalizedUsername = cred.getUsername() == null ? "" : cred.getUsername().trim();
        String normalizedPassword = plainPassword == null ? "" : plainPassword.trim();

        if (normalizedSiteName.isBlank() || normalizedUsername.isBlank() || normalizedPassword.isBlank()) {
            throw new IllegalArgumentException("Debes completar servicio, usuario y contraseña.");
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
    // Helpers para cifrar y descifrar las contraseñas del usuario
    // ------------------------------------------------------------------

    private String encrypt(String raw, Optional<User> userOpt) {
        if (raw == null) return null;
        if (!userOpt.isPresent()) {
            throw new IllegalStateException("No hay usuario en sesión para cifrar la contraseña");
        }
        User user = userOpt.get();

        // 1. Le pedimos la llave maestra al archivo temporal de la sesion, donde se ha guardado
        String userKey = user.getEncryptionKey();
        if (userKey == null) {
            throw new IllegalStateException("No hay llave de cifrado en la sesión");
        }

        try {
            // 2. Cerramos el candado AES usando la llave personal del usuario
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(userKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            
            byte[] encryptedBytes = cipher.doFinal(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encryptedBytes);
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

        // 1. Le pedimos la llave maestra al archivo temporal de la sesion, donde se ha guardado
        String userKey = user.getEncryptionKey();
        if (userKey == null) {
            throw new IllegalStateException("No hay llave de descifrado en la sesión");
        }

        try {
            // 2. Cerramos el candado AES usando la llave personal del usuario
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(userKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
            
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encrypted);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar la contraseña. ¿Llave incorrecta?", e);
        }
    }

    // ------------------------------------------------------------------
    // Re-cifrado de credenciales cuando cambia la contraseña maestra
    // ------------------------------------------------------------------

    /**
     * Deriva una clave de cifrado a partir de una contraseña maestra.
     * Utiliza SHA-256 para generar una clave de 32 caracteres (256 bits).
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
     * Encripta una contraseña usando una llave proporcionada (no la de la sesión).
     * Se usa durante la migración de contraseñas.
     */
    private String encryptWithKey(String raw, String encryptionKey) {
        if (raw == null) return null;
        
        try {
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(encryptionKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, key);
            
            byte[] encryptedBytes = cipher.doFinal(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar la contraseña", e);
        }
    }

    /**
     * Desencripta una contraseña usando una llave proporcionada (no la de la sesión).
     * Se usa durante la migración de contraseñas.
     */
    private String decryptWithKey(String encrypted, String encryptionKey) {
        if (encrypted == null) return null;
        
        try {
            java.security.Key key = new javax.crypto.spec.SecretKeySpec(encryptionKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "AES");
            javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES");
            cipher.init(javax.crypto.Cipher.DECRYPT_MODE, key);
            
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(encrypted);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar la contraseña. ¿Llave incorrecta?", e);
        }
    }

    /**
     * Re-cifra todas las credenciales de un usuario con una nueva contraseña maestra.
     * Primero desencripta con la clave antigua, luego encripta con la clave nueva.
     * 
     * @param userId ID del usuario
     * @param oldPassword Contraseña maestra anterior (sin hashear)
     * @param newPassword Contraseña maestra nueva (sin hashear)
     * @return Mensaje de error si algo falla, null si éxito
     */
    public String reEncryptAllCredentials(Long userId, String oldPassword, String newPassword) {
        try {
            // Derivar las claves de ambas contraseñas
            String oldKey = deriveEncryptionKey(oldPassword);
            String newKey = deriveEncryptionKey(newPassword);
            
            // Obtener todas las credenciales del usuario
            List<Credential> credentials = credentialRepository.findByUserId(userId);
            
            // Re-cifrar cada credencial
            for (Credential cred : credentials) {
                try {
                    // Desencriptar con la clave anterior
                    String decrypted = decryptWithKey(cred.getPasswordEncrypted(), oldKey);
                    
                    // Encriptar con la clave nueva
                    String encrypted = encryptWithKey(decrypted, newKey);
                    
                    // Actualizar la credencial
                    cred.setPasswordEncrypted(encrypted);
                    credentialRepository.save(cred);
                } catch (Exception e) {
                    // Si una credencial falla, registrar el error pero continuar
                    System.err.println("Error re-cifrando credencial " + cred.getId() + ": " + e.getMessage());
                }
            }
            
            return null; // Éxito
        } catch (Exception e) {
            return "Error al re-cifrar las credenciales: " + e.getMessage();
        }
    }
}