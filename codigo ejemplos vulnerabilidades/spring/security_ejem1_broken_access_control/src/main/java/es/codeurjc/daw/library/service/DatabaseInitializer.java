package es.codeurjc.daw.library.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.codeurjc.daw.library.model.Post;
import es.codeurjc.daw.library.model.User;
import es.codeurjc.daw.library.repository.PostRepository;
import es.codeurjc.daw.library.repository.UserRepository;
import jakarta.annotation.PostConstruct;

@Service
public class DatabaseInitializer {

	@Autowired
	private PostRepository postRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@PostConstruct
	public void init() {

		// Sample users

		User user = new User("michel", passwordEncoder.encode("pass"), "USER");
		User user2 = new User("oscar", passwordEncoder.encode("pass"), "USER");
		User admin = new User("admin", passwordEncoder.encode("adminpass"), "USER", "ADMIN");
		userRepository.save(user);
		userRepository.save(user2);
		userRepository.save(admin);

		// Sample posts

		postRepository.save(new Post("First post", "This is the content of the first post", user));
		postRepository.save(new Post("Second post", "This is the content of the second post", user2));
		postRepository.save(new Post("Third post", "This is the content of the third post", user));

	}

}
