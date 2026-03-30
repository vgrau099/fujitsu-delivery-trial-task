package com.fujitsu.delivery.repository;

import com.fujitsu.delivery.entity.WeatherObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for accessing and persisting weather observation records.
 */
@Repository
public interface WeatherObservationRepository extends JpaRepository<WeatherObservation, Long> {

    /**
     * This finds the newest weather report for a specific station.
     * It sorts them by time so the latest one is first.
     * @param stationName The name of the city (station) to look for.
     * @return The latest report, or nothing if the station isn't found.
     */
    Optional<WeatherObservation> findTopByStationNameOrderByObservationTimestampDesc(String stationName);
}