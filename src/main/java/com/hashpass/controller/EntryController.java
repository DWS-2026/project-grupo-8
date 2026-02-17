package com.hashpass.controller;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.hashpass.model.Credential;
import com.hashpass.service.EntryService;

@Controller
public class EntryController {

    @Autowired
    private EntryService entryService;

    // Listado de todas las credenciales (Rúbrica punto 3)
    @GetMapping("/passwords")
    public String listEntries(Model model) {
        List<Credential> entries = entryService.findAll();
        model.addAttribute("entries", entries);
        return "passwords"; // Debe coincidir con tu passwords.html
    }

    // Crear nueva credencial (Rúbrica punto 5)
    @PostMapping("/entry/new")
    public String newEntry(Credential credential, @RequestParam("imageFile") MultipartFile imageFile) throws IOException {
        
        // Si el usuario sube una imagen, la convertimos a byte[] para la BD
        if (!imageFile.isEmpty()) {
            credential.setImage(imageFile.getBytes());
        }
        
        entryService.save(credential);
        return "redirect:/passwords";
    }

    // Ver detalle de una credencial (Rúbrica punto 4)
    @GetMapping("/entry/{id}")
    public String showEntry(Model model, @PathVariable long id) {
        Credential entry = entryService.findById(id);
        if (entry != null) {
            model.addAttribute("entry", entry);
            return "info-passwords"; 
        }
        return "error"; // Página de error si no existe (Rúbrica punto 12)
    }

    // Borrado de credencial (Rúbrica punto 7)
    @PostMapping("/entry/{id}/delete")
    public String deleteEntry(@PathVariable long id) {
        entryService.deleteById(id);
        return "redirect:/passwords";
    }
}