package com.example.reviews;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReviewsApplication {
    public static void main(String[] args) {
        // 1) Load .env (by default looks for ".env" in project root)
        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing() // don’t crash if .env isn’t there
                .load();

        // 2) Push .env entries into system properties
        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        // 3) Start Spring
        SpringApplication.run(ReviewsApplication.class, args);
    }
}
