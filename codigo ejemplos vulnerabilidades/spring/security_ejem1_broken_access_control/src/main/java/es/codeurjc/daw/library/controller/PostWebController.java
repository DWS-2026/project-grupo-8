package es.codeurjc.daw.library.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import es.codeurjc.daw.library.model.Post;
import es.codeurjc.daw.library.model.User;
import es.codeurjc.daw.library.repository.UserRepository;
import es.codeurjc.daw.library.service.PostService;
import es.codeurjc.daw.library.service.UserService;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PostWebController {

	@Autowired
	private PostService service;

	@Autowired
	private UserService userService;

	@ModelAttribute
	public void addAttributes(Model model, HttpServletRequest request) {

		Principal principal = request.getUserPrincipal();

		if (principal != null) {
			model.addAttribute("logged", true);
			model.addAttribute("userName", principal.getName());
			model.addAttribute("admin", request.isUserInRole("ADMIN"));
		} else {
			model.addAttribute("logged", false);
		}
	}

	@GetMapping("/")
	public String showPosts(Model model) {
		model.addAttribute("posts", service.findAll());
		return "post/list";
	}

	@GetMapping("/posts/{id}")
	public String showPost(Model model, @PathVariable long id, HttpServletRequest request) {
		Optional<Post> post = service.findById(id);
		if (post.isPresent()) {
			model.addAttribute("post", post.get());
			Optional<User> user = userService.getLoggedUser();
			if(user.isPresent()) {
				String currentUsername = user.get().getName();
				boolean isOwner = currentUsername.equals(post.get().getAuthor().getName()) || user.get().getRoles().contains("ADMIN");
				model.addAttribute("isOwner", isOwner);
			}else {
				model.addAttribute("isOwner", false);
			}
			return "post/view";
		} else {
			return "redirect:/";
		}
	}

	@PostMapping("/removepost/{id}")
	public String removePost(Model model, @PathVariable long id) {
		Optional<Post> post = service.findById(id);
		if (post.isPresent()) {
			service.delete(id);
			model.addAttribute("post", post.get());
			return "post/removed-message";
		} else {
			return "redirect:/";
		}
	}

	@GetMapping("/newpost")
	public String newPost(Model model) {
		return "post/new";
	}

	@PostMapping("/newpost")
	public String newPostProcess(Model model, Post post, Principal principal) {
		service.save(post);
		model.addAttribute("postId", post.getId());
		return "post/created-message";
	}

	@GetMapping("/editpost/{id}")
	public String editPost(Model model, @PathVariable long id) {
		Optional<Post> post = service.findById(id);
		if (post.isPresent()) {
			model.addAttribute("post", post.get());
			return "post/edit";
		} else {
			return "redirect:/";
		}
	}

	@PostMapping("/editpost")
	public String editPostProcess(Model model, Post post) {
		Optional<Post> existing = service.findById(post.getId());
		if (existing.isPresent()) {
			Post existingPost = existing.get();
			existingPost.setTitle(post.getTitle());
			existingPost.setText(post.getText());
			service.save(existingPost);
			model.addAttribute("postId", existingPost.getId());
			return "post/edited-message";
		} else {
			return "redirect:/";
		}
	}

}
