package com.example.reviews.service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class StartupImporter implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(StartupImporter.class);
    private final ImportService importService;

    public StartupImporter(ImportService importService) {
        this.importService = importService;
    }

    @Override
    public void run(String... args) {
        int count = importService.importAll();
        log.info("Startup import complete: {} items", count);
    }
}
