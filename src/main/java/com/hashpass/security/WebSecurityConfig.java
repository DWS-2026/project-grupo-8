package com.hashpass.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.hashpass.service.UserSession;
import java.time.LocalDateTime;
import com.hashpass.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	@Autowired
	RepositoryUserDetailsService userDetailsService;

	@Autowired
	UserSession userSession;

	@Autowired
	UserRepository userRepository;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
		return authConfig.getAuthenticationManager();
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
                        .requestMatchers("/login").permitAll()
                        .requestMatchers("/register").permitAll()
                        .requestMatchers("/password-login").permitAll()
						.requestMatchers("/error", "/error/**").permitAll()
						.requestMatchers("/css/**").permitAll()
						.requestMatchers("/images/**").permitAll()
						.requestMatchers("/reviews/**").permitAll()
                        .requestMatchers("/footerPublic/**").permitAll()
                        .requestMatchers("/head/*").permitAll()
                        .requestMatchers("/navbarPublic/*").permitAll()
                        .requestMatchers("/assets/**").permitAll() // Allow access to static resources
						// PRIVATE PAGES
					.requestMatchers("/add-password", "/add-password/**").hasAnyRole("USER")
					.requestMatchers("/delete-password", "/delete-password/**").hasAnyRole("USER")
					.requestMatchers("/save-password-edit", "/save-password-edit/**").hasAnyRole("USER")
						.requestMatchers("/config-user", "/config-user/**").hasAnyRole("USER")
						.requestMatchers("/dashboard", "/dashboard/**").hasAnyRole("USER")
                        .requestMatchers("/info-passwords", "/info-passwords/**").hasAnyRole("USER")
                        .requestMatchers("/index", "/index/**").hasAnyRole("USER")
                        .requestMatchers("/passwords", "/passwords/**").hasAnyRole("USER")
                        .requestMatchers("/payment", "/payment/**").hasAnyRole("USER")
                        .requestMatchers("/plan", "/plan/**").hasAnyRole("USER")
                        .requestMatchers("/security-user", "/security-user/**").hasAnyRole("USER")
                        .requestMatchers("/user", "/user/**").hasAnyRole("USER")
						.requestMatchers("/footerPrivate", "/footerPrivate/**").hasAnyRole("USER")
						.requestMatchers("/navbarPrivate", "/navbarPrivate/**").hasAnyRole("USER")
						.requestMatchers("/sidebar", "/sidebar/**").hasAnyRole("USER")


                        .requestMatchers("/admin_user_detail/*").hasAnyRole("ADMIN")
                        .requestMatchers("/admin/*").hasAnyRole("ADMIN"))

                        .formLogin(formLogin -> formLogin
						.loginPage("/login")
						.usernameParameter("email")
						.passwordParameter("password")
						.failureHandler((request, response, exception) -> {
							String email = request.getParameter("email");
							if (email == null) {
								email = "";
							}
							String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
							response.sendRedirect("/password-login?email=" + encodedEmail + "&error=1");
						})
						.defaultSuccessUrl("/dashboard")
						.successHandler((request, response, auth) -> {
							String password = request.getParameter("password");
							String email = auth.getName();
							userRepository.findByEmail(email).ifPresent(user -> {
								userSession.setUser(user);
								userSession.setEncryptionKey(deriveKey(password));

								// Actualizar contador de logins de 30 días (simple, persistente en User)
								LocalDateTime now = LocalDateTime.now();
								if (user.getLoginCountWindowStart() == null || user.getLoginCountWindowStart().isBefore(now.minusDays(30))) {
									user.setLoginCount(1);
									user.setLoginCountWindowStart(now);
								} else {
									user.setLoginCount((user.getLoginCount() == null ? 0 : user.getLoginCount()) + 1);
								}
								userRepository.save(user);
							});
							response.sendRedirect("/dashboard");
						})
						.permitAll())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.accessDeniedPage("/error/403"))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/")
						.addLogoutHandler((request, response, auth) -> userSession.logout())
						.permitAll());

        http.csrf(csrf -> csrf.disable());
		return http.build();
	}

	private String deriveKey(String password) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString().substring(0, 32);
		} catch (Exception e) {
			throw new RuntimeException("Error generando la llave maestra", e);
		}
	}
}