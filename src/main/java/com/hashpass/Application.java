package com.hashpass;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.hashpass.service.DatabaseInitializer;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	/**
	 * Inicializa la base de datos después de que la aplicación está completamente compilada.
	 * 
	 * @param databaseInitializer Service encargado de la inicialización
	 * @return CommandLineRunner que ejecuta la inicialización
	 */
	@Bean
	public CommandLineRunner seedData(DatabaseInitializer databaseInitializer) {
		return args -> databaseInitializer.initializeDatabase();
	}
}