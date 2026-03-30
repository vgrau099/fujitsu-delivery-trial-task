package com.fujitsu.delivery.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * This class stores a single weather report.
 * It saves a new record every time data is downloaded to keep a full history.
 */
@Entity
@Table(name = "weather_observation")
@Getter
@Setter
@NoArgsConstructor
public class WeatherObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Station name ("Tallinn-Harku"). */
    @Column(nullable = false)
    private String stationName;

    /** WMO code. */
    @Column(nullable = false)
    private String wmoCode;

    /** Air temp. (Celsius). */
    private Double airTemperature;

    /** Wind speed (meters/second). */
    private Double windSpeed;

    /** Text desc. ("Light snow shower"). */
    private String weatherPhenomenon;

    /**
     * The exact time the weather station measured the data.
     * The computer reads this as a long number of seconds from the XML.
     */
    @Column(nullable = false)
    private LocalDateTime observationTimestamp;

    /** Timestamp when this record was inserted into the database. */
    @Column(nullable = false)
    private LocalDateTime importedAt;
}