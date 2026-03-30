package com.fujitsu.delivery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables Spring's scheduled task execution.
 * Req. for the {@link com.fujitsu.delivery.scheduler.WeatherImportScheduler} to run.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
}