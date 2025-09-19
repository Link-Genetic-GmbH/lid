package org.linkgenetic.resolver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * LinkID Resolver - Spring Boot Application
 *
 * A high-performance, enterprise-grade resolver for LinkID persistent identifiers.
 * Implements the LinkID resolution specification with advanced caching, authentication,
 * federated resolver discovery, and semantic resolution capabilities.
 *
 * @author Link Genetic GmbH
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableWebSecurity
@EnableMongoRepositories
@ConfigurationPropertiesScan
public class LinkIdResolverApplication {

    /**
     * Main entry point for the LinkID Resolver application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Set system properties for optimal performance
        System.setProperty("spring.jpa.open-in-view", "false");
        System.setProperty("spring.web.locale-resolver", "fixed");

        SpringApplication app = new SpringApplication(LinkIdResolverApplication.class);

        // Configure Spring Boot banner
        app.setBannerMode(org.springframework.boot.Banner.Mode.CONSOLE);

        // Run the application
        app.run(args);
    }
}