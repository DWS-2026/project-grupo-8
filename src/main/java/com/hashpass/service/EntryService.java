package com.hashpass.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hashpass.model.Credential;
import com.hashpass.model.User;
import com.hashpass.repository.CredentialRepository;

@Service
public class EntryService {

    @Autowired
    private CredentialRepository credentialRepository;

    @Autowired
    private UserSession userSession;

    /**
     * Obtiene todas las credenciales pertenecientes al usuario actualmente
     * almacenado en la sesión. Si no hay usuario lanza IllegalStateException.
     */
    public List<Credential> listCurrentUser() {
        User u = userSession.getUser();
        if (u == null) {
            throw new IllegalStateException("No hay usuario en sesión");
        }
        return credentialRepository.findByUserId(u.getId());
    }

    /**
     * Guarda una nueva credencial o actualiza una existente. El parámetro
     * plainPassword es cifrado/obfuscado antes de persistir.
     */
    public Credential save(Credential cred, String plainPassword) {
        User u = userSession.getUser();
        if (u == null) {
            throw new IllegalStateException("Debe iniciar sesión para guardar credenciales");
        }
        cred.setUser(u);
        cred.setPasswordEncrypted(encrypt(plainPassword));
        return credentialRepository.save(cred);
    }

    public Optional<Credential> findById(Long id) {
        return credentialRepository.findById(id);
    }

    public void delete(Long id) {
        credentialRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // Helpers para cifrar y descifrar las contraseñas del usuario
    // ------------------------------------------------------------------

    private String encrypt(String raw) {
        if (raw == null) return null;
        
        // 1. Le pedimos la llave maestra al archivo temporal de la sesion, donde se ha guardado
        String userKey = userSession.getEncryptionKey();
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

    public String decrypt(String encrypted) {
        if (encrypted == null) return null;
        
        // 1. Le pedimos la llave maestra al archivo temporal de la sesion, donde se ha guardado
        String userKey = userSession.getEncryptionKey();
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
}