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
	 * inicialize the database after the application is fully compiled and ready
	 * 
	 * @param databaseInitializer Service that initializes the database with default data
	 * @return CommandLineRunner that runs the application and initializes the database
	 */
	@Bean
	public CommandLineRunner seedData(DatabaseInitializer databaseInitializer) {
		return args -> databaseInitializer.initializeDatabase();
	}
}