package com.hashpass.repository;

import com.hashpass.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Método extra para buscar por email (necesario para login)
    User findByEmail(String email);
}