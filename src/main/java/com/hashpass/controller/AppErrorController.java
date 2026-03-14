package com.hashpass.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppErrorController {

	@GetMapping("/error/403")
	public String accessDenied(Model model) {
		populateErrorModel(model, 403);
		return "error";
	}

	@GetMapping("/error/404")
	public String notFound(Model model) {
		populateErrorModel(model, 404);
		return "error";
	}

	@GetMapping("/error/500")
	public String serverError(Model model) {
		populateErrorModel(model, 500);
		return "error";
	}

	private void populateErrorModel(Model model, int statusCode) {
		model.addAttribute("statusCode", statusCode);

		switch (statusCode) {
			case 403:
				model.addAttribute("errorTitle", "Acceso denegado");
				model.addAttribute("errorMessage", "No tienes permisos para entrar en esta página o realizar esta acción.");
				model.addAttribute("errorIcon", "bi-shield-lock");
				model.addAttribute("primaryActionHref", "/dashboard");
				model.addAttribute("primaryActionText", "Ir al panel");
				break;
			case 404:
				model.addAttribute("errorTitle", "Página no encontrada");
				model.addAttribute("errorMessage", "La ruta que intentas abrir no existe o ya no está disponible.");
				model.addAttribute("errorIcon", "bi-signpost-split");
				model.addAttribute("primaryActionHref", "/");
				model.addAttribute("primaryActionText", "Volver al inicio");
				break;
			default:
				model.addAttribute("errorTitle", "Ha ocurrido un error");
				model.addAttribute("errorMessage", "La aplicación no ha podido completar tu solicitud en este momento.");
				model.addAttribute("errorIcon", "bi-exclamation-octagon");
				model.addAttribute("primaryActionHref", "/");
				model.addAttribute("primaryActionText", "Ir al inicio");
				break;
		}
	}
}