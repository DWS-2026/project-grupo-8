package es.codeurjc.library.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.codeurjc.library.domain.Book;
import es.codeurjc.library.repository.BookRepository;
import jakarta.annotation.PostConstruct;

@Service
public class SampleDataService {
    
    @Autowired
    private BookRepository bookRepository;

    @PostConstruct
    public void initData() {

        bookRepository.save(new Book("The Lord of the Rings", "An epic fantasy novel by J.R.R. Tolkien.", 1954, "en"));
        bookRepository.save(new Book("Don Quixote", "A Spanish novel by Miguel de Cervantes.", 1605, "es"));
        bookRepository.save(new Book("The Great Gatsby", "A novel by F. Scott Fitzgerald.", 1925, "en"));
        bookRepository.save(new Book("One Hundred Years of Solitude", "A novel by Gabriel García Márquez.", 1967, "es"));
		
    }
}
