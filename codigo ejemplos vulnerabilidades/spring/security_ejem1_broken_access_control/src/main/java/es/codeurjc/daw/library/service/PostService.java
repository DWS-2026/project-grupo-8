package es.codeurjc.daw.library.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.codeurjc.daw.library.model.Post;
import es.codeurjc.daw.library.repository.PostRepository;

@Service
public class PostService {

	@Autowired
	private PostRepository repository;

	@Autowired
	private UserService userService;

	public Optional<Post> findById(long id) {
		return repository.findById(id);
	}

	public boolean exist(long id) {
		return repository.existsById(id);
	}

	public List<Post> findAll() {
		return repository.findAll();
	}

	public void save(Post post) {
		post.setAuthor(userService.getLoggedUser().get());
		repository.save(post);
	}

	public void delete(long id) {
		repository.deleteById(id);
	}
	
}
