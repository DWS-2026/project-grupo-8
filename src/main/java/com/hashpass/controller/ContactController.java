package com.hashpass.controller;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hashpass.model.ContactRequest;
import com.hashpass.model.User;
import com.hashpass.repository.ContactRequestRepository;
import com.hashpass.service.ImageService;
import com.hashpass.service.UserService;

@Controller
public class ContactController {

	private static final DateTimeFormatter CONTACT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	private final ContactRequestRepository contactRequestRepository;
	private final UserService userService;
	private final ImageService imageService;

	public ContactController(ContactRequestRepository contactRequestRepository, UserService userService,
			ImageService imageService) {
		this.contactRequestRepository = contactRequestRepository;
		this.userService = userService;
		this.imageService = imageService;
	}

	@ModelAttribute("user")
	public User populateUser() {
		return userService.getLoggedUser().orElse(null);
	}

	@ModelAttribute("profileImageUrl")
	public String populateProfileImageUrl() {
		return imageService.getProfileImageUrl(userService.getLoggedUser());
	}

	@ModelAttribute("isLogged")
	public boolean populateIsLogged() {
		return userService.getLoggedUser().isPresent();
	}

	@GetMapping("/contact")
	public String contactForm(Model model) {
		model.addAttribute("contactRequest", new ContactRequest());
		return "contact";
	}

	@PostMapping("/contact")
	public String submitContact(@RequestParam String name, @RequestParam String email, @RequestParam String subject,
			@RequestParam String message, RedirectAttributes redirectAttributes) {

		ContactRequest contact = new ContactRequest();
		contact.setName(name);
		contact.setEmail(email);
		contact.setSubject(subject);
		contact.setMessage(message);

		contactRequestRepository.save(contact);

		redirectAttributes.addAttribute("success", "true");
		return "redirect:/contact";
	}

	@GetMapping("/admin/contacts")
	public String adminContacts(Model model, @RequestParam(name = "readId", required = false) Long readId,
			@RequestParam(name = "deleteId", required = false) Long deleteId) {
		if (!userService.getLoggedUser().isPresent()) {
			return "redirect:/login";
		}
		if (!userService.getLoggedUser().get().isAdmin()) {
			return "redirect:/error/403";
		}

		if (readId != null) {
			contactRequestRepository.findById(readId).ifPresent(contact -> {
				contact.setIsRead(true);
				contactRequestRepository.save(contact);
			});
			return "redirect:/admin/contacts";
		}

		if (deleteId != null) {
			contactRequestRepository.deleteById(deleteId);
			return "redirect:/admin/contacts";
		}

		List<ContactRequest> contacts = contactRequestRepository.findAllByOrderByCreatedAtDesc();
		model.addAttribute("contactsList", buildContactsView(contacts));
		model.addAttribute("contactsCount", contacts.size());
		model.addAttribute("unreadCount", contacts.stream().filter(c -> !c.getIsRead()).count());

		return "admin_contacts";
	}

	private List<Map<String, Object>> buildContactsView(List<ContactRequest> contacts) {
		List<Map<String, Object>> views = new ArrayList<>();

		for (ContactRequest contact : contacts) {
			Map<String, Object> view = new HashMap<>();
			view.put("id", contact.getId());
			view.put("name", contact.getName());
			view.put("email", contact.getEmail());
			view.put("subject", contact.getSubject());
			view.put("message", contact.getMessage());
			view.put("createdAt", contact.getCreatedAt().format(CONTACT_DATE_FORMAT));
			view.put("isRead", contact.getIsRead());
			views.add(view);
		}

		return views;
	}
}
