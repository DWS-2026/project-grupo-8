package com.hashpass.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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
        cred.setPasswordEncrypted(obfuscate(plainPassword));
        return credentialRepository.save(cred);
    }

    public Optional<Credential> findById(Long id) {
        return credentialRepository.findById(id);
    }

    public void delete(Long id) {
        credentialRepository.deleteById(id);
    }

    // ------------------------------------------------------------------
    // helpers simples para "cifrado"
    // ------------------------------------------------------------------

    private String obfuscate(String raw) {
        if (raw == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public String deobfuscate(String encoded) {
        if (encoded == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
    }
}