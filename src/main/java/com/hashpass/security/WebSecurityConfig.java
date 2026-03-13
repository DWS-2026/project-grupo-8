package com.hashpass.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Autowired
	RepositoryUserDetailsService userDetailsService;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public DaoAuthenticationProvider authenticationProvider() {
		DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
		authProvider.setPasswordEncoder(passwordEncoder());

		return authProvider;
	}

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http.authenticationProvider(authenticationProvider());

		http
				.authorizeHttpRequests(authorize -> authorize
						// PUBLIC PAGES
						.requestMatchers("/").permitAll()
                        .requestMatchers("/register").permitAll()
						.requestMatchers("/css/**").permitAll()
						.requestMatchers("/images/**").permitAll()
						.requestMatchers("/reviews/**").permitAll()
                        .requestMatchers("/footerPublic/**").permitAll()
                        .requestMatchers("/head/*").permitAll()
                        .requestMatchers("/navbarPublic/*").permitAll()
                        .requestMatchers("/assets/**").permitAll() // Allow access to static resources
						// PRIVATE PAGES
						.requestMatchers("/add-passwords").hasAnyRole("USER")
						.requestMatchers("/config_user").hasAnyRole("USER")
						.requestMatchers("/dashboard/*").hasAnyRole("USER")
                        .requestMatchers("/info-passwords/*").hasAnyRole("USER")
                        .requestMatchers("/index/*").hasAnyRole("USER")
                        .requestMatchers("/passwords/*").hasAnyRole("USER")
                        .requestMatchers("/payment/*").hasAnyRole("USER")
                        .requestMatchers("/plan/*").hasAnyRole("USER")
                        .requestMatchers("/security_user/*").hasAnyRole("USER")
                        .requestMatchers("/user/*").hasAnyRole("USER")
						.requestMatchers("/footerPrivate/*").hasAnyRole("USER")
						.requestMatchers("/navbarPrivate/*").hasAnyRole("USER")
						.requestMatchers("/sidebar/*").hasAnyRole("USER")


                        .requestMatchers("/admin_user_detail/*").hasAnyRole("ADMIN")
                        .requestMatchers("/admin/*").hasAnyRole("ADMIN"))

                        .formLogin(formLogin -> formLogin
						.loginPage("/login")
                        .loginPage("/password-login")
						.failureUrl("/loginerror")
						.defaultSuccessUrl("/")
						.permitAll())
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.permitAll());

        http.csrf(csrf -> csrf.disable());
		return http.build();
	}
}