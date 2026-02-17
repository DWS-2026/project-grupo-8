package com.hashpass.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "user_table") // 'user' es palabra reservada en MySQL
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;
    private String email;
    private String encodedPassword;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] profileImage;

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private List<Credential> credentials; // Un usuario tiene muchas credenciales [cite: 61]

    public User() {}

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getEncodedPassword() {
        return encodedPassword;
    }

    public byte[] getProfileImage() {
        return profileImage;
    }

    public List<Credential> getCredentials() {
        return credentials;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setEncodedPassword(String encodedPassword) {
        this.encodedPassword = encodedPassword;
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage;
    }

    public void setCredentials(List<Credential> credentials) {
        this.credentials = credentials;
    }
    
    
}