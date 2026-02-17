package com.hashpass.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Column;

@Entity
public class Credential {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String url; 
    private String username;
    private String password;

    @Lob
    @Column(columnDefinition = "LONGBLOB") // Importante para MySQL y archivos grandes
    private byte[] image; 

    @ManyToOne
    private User owner; 

    // Constructor vacío obligatorio para JPA 
    public Credential() {}

    public Credential(String url, String username, String password, User owner) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.owner = owner;
    }

	public Long getId() {
		return id;
	}

	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public byte[] getImage() {
		return image;
	}

	public User getOwner() {
		return owner;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setImage(byte[] image) {
		this.image = image;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

    // Genera aquí los Getters y Setters (imprescindibles)
}