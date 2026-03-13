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

import com.hashpass.service.UserSession;
import com.hashpass.model.User;
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
						.requestMatchers("/css/**").permitAll()
						.requestMatchers("/images/**").permitAll()
						.requestMatchers("/reviews/**").permitAll()
                        .requestMatchers("/footerPublic/**").permitAll()
                        .requestMatchers("/head/*").permitAll()
                        .requestMatchers("/navbarPublic/*").permitAll()
                        .requestMatchers("/assets/**").permitAll() // Allow access to static resources
						// PRIVATE PAGES
					.requestMatchers("/add-password", "/add-password/**").hasAnyRole("USER")
						.requestMatchers("/config-user").hasAnyRole("USER")
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
						.failureUrl("/password-login")
						.defaultSuccessUrl("/dashboard")
						.successHandler((request, response, auth) -> {
							String password = request.getParameter("password");
							String email = auth.getName();
							userRepository.findByEmail(email).ifPresent(user -> {
								userSession.setUser(user);
								userSession.setEncryptionKey(deriveKey(password));
							});
							response.sendRedirect("/dashboard");
						})
						.permitAll())
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