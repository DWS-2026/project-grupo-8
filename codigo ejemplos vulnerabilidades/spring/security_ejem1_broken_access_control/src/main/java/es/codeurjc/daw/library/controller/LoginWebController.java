package es.codeurjc.daw.library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginWebController {
	
	@GetMapping("/login")
	public String login() {
		return "session/login";
	}

	@GetMapping("/loginerror")
	public String loginerror() {
		return "session/loginerror";
	}
}
