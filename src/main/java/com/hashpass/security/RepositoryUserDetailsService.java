package com.hashpass.security;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.hashpass.model.User;
import com.hashpass.repository.UserRepository;

@Service
public class RepositoryUserDetailsService implements UserDetailsService {

	private static final int MAX_FAILED_ATTEMPTS = 5;

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		LocalDateTime lockedUntil = user.getLockedUntil();
		if (lockedUntil != null) {
			if (lockedUntil.isAfter(LocalDateTime.now())) {
				throw new LockedException("Account locked until " + lockedUntil);
			}
			user.setLockedUntil(null);
			user.setFailedAttempts(0);
			userRepository.save(user);
		} else if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
			user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
			userRepository.save(user);
			throw new LockedException("Account locked due to too many failed attempts");
		}

		List<GrantedAuthority> roles;
		if (user.isAdmin()) {
			roles = List.of(new SimpleGrantedAuthority("ROLE_USER"),
					new SimpleGrantedAuthority("ROLE_ADMIN"));
		} else {
			roles = List.of(new SimpleGrantedAuthority("ROLE_USER"));
		}

		boolean accountNonLocked = user.getLockedUntil() == null || !user.getLockedUntil().isAfter(LocalDateTime.now());
		return new org.springframework.security.core.userdetails.User(user.getEmail(),
				user.getPasswordHash(), true, true, true, accountNonLocked, roles);

	}
}