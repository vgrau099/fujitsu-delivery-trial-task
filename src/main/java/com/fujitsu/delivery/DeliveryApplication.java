package com.fujitsu.delivery;

import com.fujitsu.delivery.service.WeatherImportService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the delivery fee application.
 * On startup, immediately triggers a weather data import so the app is usable right away.
 * After that, the scheduled cron job takes over (every hour at HH:15:00).
 */
@SpringBootApplication
@RequiredArgsConstructor
public class DeliveryApplication {

    private final WeatherImportService weatherImportService;

    public static void main(String[] args) {
        SpringApplication.run(DeliveryApplication.class, args);
    }

    /**
     * Runs once after the application context is fully started.
     * Imports the latest weather data immediately so the API works without waiting for the cron.
     */
    @PostConstruct
    public void importWeatherOnStartup() {
        weatherImportService.importWeatherData();
    }
}