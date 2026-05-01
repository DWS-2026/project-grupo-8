package es.codeurjc.library.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import es.codeurjc.library.domain.Book;
import jakarta.persistence.EntityManager;

@Service
public class BookService {

    @Autowired
    private EntityManager entityManager;

    public Book findById(Long id) {
        return entityManager.find(Book.class, id);
    }

    @SuppressWarnings("unchecked")
    public List<Book> findAll(Integer from, Integer to, String lang) {
        String query = "SELECT * FROM Book";
        if ((from != null && to != null) || isNotEmptyField(lang)) {
            query += " WHERE";
        }
        if (from != null && to != null) {
            query += " publication_year BETWEEN " + from + " AND " + to;
        }
        if (from != null && to != null && isNotEmptyField(lang)) {
            query += " AND";
        }
        if (isNotEmptyField(lang)) {
            query += " lang='" + lang + "'";
        }
        return (List<Book>) entityManager.createNativeQuery(query, Book.class).getResultList();
    }

    private boolean isNotEmptyField(String field) {
        return field != null && !field.isEmpty();
    }

}
