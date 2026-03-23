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
						.requestMatchers("/plan", "/plan/**").permitAll()
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
						.requestMatchers("/security-user", "/security-user/**").hasAnyRole("USER")
						.requestMatchers("/user", "/user/**").hasAnyRole("USER")
						.requestMatchers("/footerPrivate", "/footerPrivate/**").hasAnyRole("USER")
						.requestMatchers("/navbarPrivate", "/navbarPrivate/**").hasAnyRole("USER")
						.requestMatchers("/sidebar", "/sidebar/**").hasAnyRole("USER")

						.requestMatchers("/admin-user-detail", "/admin-user-detail/**").hasAnyRole("ADMIN")
						.requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN"))

				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.usernameParameter("email")
						.passwordParameter("password")
						.failureHandler((request, response, exception) -> {
							String email = request.getParameter("email");
							if (email != null) {
								userRepository.findByEmail(email).ifPresent(user -> {
									int actuales = (user.getFailedAttempts() == null) ? 0 : user.getFailedAttempts();
									user.setFailedAttempts(actuales + 1);
									userRepository.save(user);
								});
							}
							String redirectTo = sanitizeRedirectTarget(request.getParameter("redirectTo"));
							if (email == null) {
								email = "";
							}
							String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
							String redirectParam = redirectTo.isBlank()
									? ""
									: "&redirectTo=" + URLEncoder.encode(redirectTo, StandardCharsets.UTF_8);
							response.sendRedirect("/password-login?email=" + encodedEmail + "&error=1" + redirectParam);
						})
						.defaultSuccessUrl("/dashboard")
						.successHandler((request, response, auth) -> {
							String password = request.getParameter("password");
							String email = auth.getName();
							String redirectTo = sanitizeRedirectTarget(request.getParameter("redirectTo"));
							userRepository.findByEmail(email).ifPresent(user -> {
								userSession.setUser(user);
								userSession.setEncryptionKey(deriveKey(password));
								request.getSession().setMaxInactiveInterval(user.getSecurityTimeoutMinutes() * 60);
								user.setLastLogin(LocalDateTime.now()); // Fecha real de éxito
								user.setFailedAttempts(0);

								// Actualizar contador de logins de 30 días (simple, persistente en User)
								LocalDateTime now = LocalDateTime.now();
								if (user.getLoginCountWindowStart() == null
										|| user.getLoginCountWindowStart().isBefore(now.minusDays(30))) {
									user.setLoginCount(1);
									user.setLoginCountWindowStart(now);
								} else {
									user.setLoginCount((user.getLoginCount() == null ? 0 : user.getLoginCount()) + 1);
								}
								userRepository.save(user);
							});
							// redirect based on role: admins go to /admin
							userRepository.findByEmail(email).ifPresent(user -> {
								try {
									if (!redirectTo.isBlank()) {
										response.sendRedirect(redirectTo);
									} else if (user.isAdmin()) {
										response.sendRedirect("/admin");
									} else {
										response.sendRedirect("/dashboard");
									}
								} catch (java.io.IOException e) {
									throw new RuntimeException(e);
								}
							});
						})
						.permitAll())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.accessDeniedPage("/error/403"))
				.sessionManagement(sessionManagement -> sessionManagement
						.invalidSessionUrl("/login?expired=1"))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout=1")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID")
						.addLogoutHandler((request, response, auth) -> userSession.logout())
						.permitAll());
		return http.build();
	}

	private String sanitizeRedirectTarget(String redirectTo) {
		if (redirectTo == null || redirectTo.isBlank()) {
			return "";
		}
		if (!redirectTo.startsWith("/") || redirectTo.startsWith("//")) {
			return "";
		}
		return redirectTo;
	}

	private String deriveKey(String password) {
		try {
			java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
			StringBuilder hexString = new StringBuilder();
			for (byte b : hash) {
				String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1)
					hexString.append('0');
				hexString.append(hex);
			}
			return hexString.toString().substring(0, 32);
		} catch (Exception e) {
			throw new RuntimeException("Error generando la llave maestra", e);
		}
	}
}