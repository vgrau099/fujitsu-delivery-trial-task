package com.fujitsu.delivery.scheduler;

import com.fujitsu.delivery.service.WeatherImportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * This class acts as a timer for the application.
 * It tells the system when to go and download new weather data.
 * The schedule is set in the application.properties file.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherImportScheduler {

    private final WeatherImportService weatherImportService;

    /**
     * It runs automatically based on the configured schedule.
     * It tells the weather service to start importing data.
     */
    @Scheduled(cron = "${weather.import.cron}")
    public void runWeatherImport() {
        log.info("The scheduled timer started the weather import.");
        weatherImportService.importWeatherData();
    }
}