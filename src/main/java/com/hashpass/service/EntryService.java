package com.hashpass.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hashpass.model.Credential;
import com.hashpass.repository.CredentialRepository;

@Service
public class EntryService {

	@Autowired
	private CredentialRepository credentialRepository;

	// Obtener todas las credenciales de la base de datos
	public List<Credential> findAll() {
		return credentialRepository.findAll();
	}

	// Buscar una credencial por su ID (usado en la página de detalle)
	public Credential findById(long id) {
		return credentialRepository.findById(id).orElse(null);
	}

	// Guardar o actualizar (esto impacta directamente en MySQL)
	public void save(Credential credential) {
		credentialRepository.save(credential);
	}

	// Borrar de la base de datos
	public void deleteById(long id) {
		credentialRepository.deleteById(id);
	}
}