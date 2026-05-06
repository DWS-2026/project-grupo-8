package com.hashpass.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
// CSRF for API is disabled below; CookieCsrfTokenRepository removed for API chain
import org.springframework.security.authentication.LockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.hashpass.security.jwt.JwtRequestFilter;
import com.hashpass.service.UserService;
import java.time.LocalDateTime;
import com.hashpass.repository.UserRepository;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {
	private static final Logger log = LoggerFactory.getLogger(WebSecurityConfig.class);
	private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
	private static final int LOCK_MINUTES = 15;


	@Autowired
	LoginRateLimitFilter loginRateLimitFilter;
	@Autowired
	RepositoryUserDetailsService userDetailsService;

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	JwtRequestFilter jwtRequestFilter;

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
	@Order(2)
	public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {

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
						.requestMatchers("/admin", "/admin/**").hasAnyRole("ADMIN")

						// OpenAPI
						.requestMatchers("/v3/api-docs*/**").permitAll()
						.requestMatchers("/swagger-ui.html").permitAll()
						.requestMatchers("/swagger-ui/**").permitAll())

				.formLogin(formLogin -> formLogin
						.loginPage("/login")
						.usernameParameter("email")
						.passwordParameter("password")
						.failureHandler((request, response, exception) -> {
							String email = request.getParameter("email");
							String normalizedEmail = normalizeIdentifier(email);
							boolean locked = exception instanceof LockedException;
							final int[] failedAttemptsRef = new int[] { 0 };
							if (email != null) {
								userRepository.findByEmail(normalizedEmail).ifPresent(user -> {
									if (!locked) {
										int actuales = (user.getFailedAttempts() == null) ? 0 : user.getFailedAttempts();
										int failedAttempts = actuales + 1;
										failedAttemptsRef[0] = failedAttempts;
										user.setFailedAttempts(failedAttempts);
										if (failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
											user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
										}
									}
									userRepository.save(user);
								});
							}
							logLoginFailure(request, normalizedEmail, locked, failedAttemptsRef[0], exception);
							String redirectTo = sanitizeRedirectTarget(request.getParameter("redirectTo"));
							if (email == null) {
								email = "";
							}
							String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
							String errorParam = locked ? "locked=1" : "error=1";
							String redirectParam = redirectTo.isBlank()
									? ""
									: "&redirectTo=" + URLEncoder.encode(redirectTo, StandardCharsets.UTF_8);
							response.sendRedirect("/password-login?email=" + encodedEmail + "&" + errorParam + redirectParam);
						})
						.defaultSuccessUrl("/dashboard")
						.successHandler((request, response, auth) -> {
							String password = request.getParameter("password");
							String email = auth.getName();
							logLoginSuccess(request, email);
							String redirectTo = sanitizeRedirectTarget(request.getParameter("redirectTo"));
							userRepository.findByEmail(email).ifPresent(user -> {
								userService.setUser(user);
								user.setEncryptionKey(deriveKey(password));
								request.getSession().setMaxInactiveInterval(user.getSecurityTimeoutMinutes() * 60);
								user.setLastLogin(LocalDateTime.now()); // Actual successful login timestamp
								user.setFailedAttempts(0);
								user.setLockedUntil(null);

								// Update 30-day login counter (simple and persisted in User)
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
										log.error("SECURITY_EVENT=LOGIN_REDIRECT_FAILED user={} ip={} reason={} ",
												maskIdentifier(email),
												resolveClientIp(request),
												e.getClass().getSimpleName(),
												e);
									throw new RuntimeException(e);
								}
							});
						})
						.permitAll())
				.exceptionHandling(exceptionHandling -> exceptionHandling
						.accessDeniedHandler((request, response, accessDeniedException) -> {
							logAccessDenied(request, accessDeniedException);
							response.sendRedirect("/error/403");
						}))
				.sessionManagement(sessionManagement -> sessionManagement
						.invalidSessionUrl("/login?expired=1"))
				.logout(logout -> logout
						.logoutUrl("/logout")
						.logoutSuccessUrl("/login?logout=1")
						.invalidateHttpSession(true)
						.clearAuthentication(true)
						.deleteCookies("JSESSIONID")
						.addLogoutHandler((request, response, auth) -> userService.logout())
						.permitAll());
		http.addFilterBefore(loginRateLimitFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
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

	@Bean
	@Order(1)
	public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {

		http.authenticationProvider(authenticationProvider());

		http
				.securityMatcher("/api/**");
				//.exceptionHandling(handling -> handling.authenticationEntryPoint(unauthorizedHandlerJwt));

		http.exceptionHandling(handling -> handling
				.authenticationEntryPoint((request, response, authException) -> {
					logUnauthorized(request, authException);
					response.sendError(401, "Unauthorized");
				})
				.accessDeniedHandler((request, response, accessDeniedException) -> {
					logAccessDenied(request, accessDeniedException);
					response.sendError(403, "Forbidden");
				}));

		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers(HttpMethod.POST, "/api/v1/users/login").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()

						// Reviews API
						.requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/reviews/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/reviews/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/reviews/**").hasAnyRole("USER", "ADMIN")
						
						// Images API
						.requestMatchers(HttpMethod.GET, "/api/v1/images/profiles").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/images/credentials").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/images/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/images/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/images/**").hasAnyRole("USER", "ADMIN")
						
						
						// Credentials API
						.requestMatchers(HttpMethod.GET, "/api/v1/credentials/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/v1/credentials/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/credentials/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/credentials/**").hasAnyRole("USER", "ADMIN")
						
						// Users API
						.requestMatchers(HttpMethod.GET, "/api/v1/users").hasRole("ADMIN")
						.requestMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.POST, "/api/v1/users/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/users/**").hasAnyRole("USER", "ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasAnyRole("USER", "ADMIN")

						// Plans API
						.requestMatchers(HttpMethod.GET, "/api/v1/plans/**").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/plans/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.PUT, "/api/v1/plans/**").hasRole("ADMIN")
						.requestMatchers(HttpMethod.DELETE, "/api/v1/plans/**").hasRole("ADMIN")
						.anyRequest().authenticated());

		// Disable form login for REST endpoints and use HTTP Basic for API clients
		http.formLogin(AbstractHttpConfigurer::disable);

		// Disable CSRF for API endpoints (API clients authenticate differently).
		http.csrf(AbstractHttpConfigurer::disable);

		http.httpBasic(Customizer.withDefaults());

		// Stateless session
		http.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// Add JWT Token filter
		http.addFilterBefore(jwtRequestFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	private void logLoginSuccess(HttpServletRequest request, String principal) {
		log.info("SECURITY_EVENT=LOGIN_SUCCESS user={} ip={} path={} userAgent={}",
				maskIdentifier(principal),
				resolveClientIp(request),
				request.getRequestURI(),
				sanitizeUserAgent(request.getHeader("User-Agent")));
	}

	private void logLoginFailure(HttpServletRequest request, String principal, boolean locked, int failedAttempts,
				Exception exception) {
		log.warn("SECURITY_EVENT=LOGIN_FAILURE user={} ip={} path={} locked={} failedAttempts={} reason={} userAgent={}",
				maskIdentifier(principal),
				resolveClientIp(request),
				request.getRequestURI(),
				locked,
				failedAttempts,
				exception.getClass().getSimpleName(),
				sanitizeUserAgent(request.getHeader("User-Agent")));
	}

	private void logUnauthorized(HttpServletRequest request, Exception exception) {
		log.warn("SECURITY_EVENT=UNAUTHORIZED ip={} path={} reason={} userAgent={}",
				resolveClientIp(request),
				request.getRequestURI(),
				exception.getClass().getSimpleName(),
				sanitizeUserAgent(request.getHeader("User-Agent")));
	}

	private void logAccessDenied(HttpServletRequest request, Exception exception) {
		String principal = request.getUserPrincipal() == null ? "anonymous" : request.getUserPrincipal().getName();
		log.warn("SECURITY_EVENT=ACCESS_DENIED user={} ip={} path={} reason={} userAgent={}",
				maskIdentifier(principal),
				resolveClientIp(request),
				request.getRequestURI(),
				exception.getClass().getSimpleName(),
				sanitizeUserAgent(request.getHeader("User-Agent")));
	}

	private String normalizeIdentifier(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().toLowerCase();
	}

	private String maskIdentifier(String value) {
		if (value == null || value.isBlank()) {
			return "unknown";
		}
		String trimmed = value.trim();
		if (trimmed.length() <= 2) {
			return "**";
		}
		return trimmed.substring(0, 2) + "***";
	}

	private String resolveClientIp(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			int commaIndex = forwarded.indexOf(',');
			return commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
		}
		return request.getRemoteAddr();
	}

	private String sanitizeUserAgent(String userAgent) {
		if (userAgent == null || userAgent.isBlank()) {
			return "unknown";
		}
		String trimmed = userAgent.trim();
		return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
	}
}