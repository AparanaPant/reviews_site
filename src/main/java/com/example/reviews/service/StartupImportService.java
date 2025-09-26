package com.example.reviews.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Runs once at application startup to trigger an initial reviews import.
 * Useful for ensuring the database is preloaded before the app starts serving requests.
 * Can be disabled or replaced later if scheduled/continuous imports are introduced.
 */
@Component
public class StartupImportService implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupImportService.class);
    private final ReviewImportService reviewImportService;

    public StartupImportService(ReviewImportService reviewImportService) {
        this.reviewImportService = reviewImportService;
    }

    @Override
    public void run(String... args) {
        int count = reviewImportService.importAll();
        log.info("Startup import complete: {} items", count);
    }
}
