package es.codeurjc.daw.library.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import es.codeurjc.daw.library.model.Post;

public interface PostRepository extends JpaRepository<Post, Long> {

}