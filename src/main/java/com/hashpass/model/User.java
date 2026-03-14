package com.hashpass.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(nullable = false, length = 150)
	private String email;

	@Column(nullable = false, length = 255)
	private String passwordHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "plan_id")
	private Plan plan;

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Credential> credentials = new ArrayList<>();

	@OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<Review> reviews = new ArrayList<>();

	@jakarta.persistence.OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private Image profileImage;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@Column
	private Integer loginCount;

	@Column
	private LocalDateTime loginCountWindowStart;

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	public List<Credential> getCredentials() {
		return credentials;
	}

	public void setCredentials(List<Credential> credentials) {
		this.credentials = credentials;
	}

	public List<Review> getReviews() {
		return reviews;
	}

	public void setReviews(List<Review> reviews) {
		this.reviews = reviews;
	}

	public Image getProfileImage() {
		return profileImage;
	}

	public void setProfileImage(Image profileImage) {
		this.profileImage = profileImage;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

	public Integer getLoginCount() {
		return loginCount;
	}

	public void setLoginCount(Integer loginCount) {
		this.loginCount = loginCount;
	}

	public LocalDateTime getLoginCountWindowStart() {
		return loginCountWindowStart;
	}

	public void setLoginCountWindowStart(LocalDateTime loginCountWindowStart) {
		this.loginCountWindowStart = loginCountWindowStart;
	}

	// Computed properties for dynamic dashboard stats
	public String getSecurity() {
		if (credentials.isEmpty()) {
			return "Excelente";
		}
		
		long weakCount = credentials.stream()
				.filter(c -> c.getPasswordEncrypted().length() < 8)
				.count();
		
		double weakPercentage = (double) weakCount / credentials.size() * 100;
		
		if (weakPercentage == 0) {
			return "Excelente";
		} else if (weakPercentage <= 25) {
			return "Buena";
		} else if (weakPercentage <= 50) {
			return "Regular";
		} else {
			return "Débil";
		}
	}

	public String getTenure() {
		if (createdAt == null) {
			return "N/A";
		}
		
		long monthsDiff = java.time.temporal.ChronoUnit.MONTHS.between(createdAt, LocalDateTime.now());
		
		if (monthsDiff == 0) {
			return "Reciente";
		} else if (monthsDiff < 12) {
			return monthsDiff + " mes" + (monthsDiff > 1 ? "es" : "");
		} else {
			long yearsDiff = monthsDiff / 12;
			return yearsDiff + " año" + (yearsDiff > 1 ? "s" : "");
		}
	}

	public String getTwoFactorAuth() {
		return "Desactivo";
	}

	public String getPaymentStatus() {
		return "Activo";
	}
}
