package com.hashpass.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/css/**", "/images/**", "/", "/register", "/plan").permitAll() // Público [cite: 301]
                .requestMatchers("/admin/**").hasRole("ADMIN") // Solo Admin [cite: 51, 313]
                .anyRequest().authenticated() // El resto requiere login [cite: 310]
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/passwords", true)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .permitAll()
            );

        return http.build();
    }
}