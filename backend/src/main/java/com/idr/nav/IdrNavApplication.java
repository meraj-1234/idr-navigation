package com.idr.nav;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application entry point for the AI/ML-based Intelligent Dead Reckoning (IDR) System.
 */
@SpringBootApplication
@EnableScheduling
public class IdrNavApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdrNavApplication.class, args);
    }
}
