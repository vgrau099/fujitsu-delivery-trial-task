package com.fujitsu.delivery.service;

import com.fujitsu.delivery.entity.WeatherObservation;
import com.fujitsu.delivery.exception.ForbiddenVehicleException;
import com.fujitsu.delivery.repository.WeatherObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * This service calculates the total delivery fee.
 * It uses the city, the vehicle type, and the latest weather from the database
 * to figure out the final price.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeliveryFeeService {

    private final WeatherObservationRepository weatherObservationRepository;

    /**
     * It matches a city name to the specific station name used by the weather agency.
     */
    private String resolveStationName(String city) {
        return switch (city.toLowerCase()) {
            case "tallinn" -> "Tallinn-Harku";
            case "tartu"   -> "Tartu-Tõravere";
            case "pärnu", "parnu" -> "Pärnu";
            default -> throw new IllegalArgumentException("Unsupported city: " + city);
        };
    }

    /**
     * It finds the base fee (RBF) based on the city and what vehicle is being used.
     */
    private double calculateRegionalBaseFee(String city, String vehicleType) {
        return switch (city.toLowerCase()) {
            case "tallinn" -> switch (vehicleType.toLowerCase()) {
                case "car"     -> 4.0;
                case "scooter" -> 3.5;
                case "bike"    -> 3.0;
                default -> throw new IllegalArgumentException("Unsupported vehicle type: " + vehicleType);
            };
            case "tartu" -> switch (vehicleType.toLowerCase()) {
                case "car"     -> 3.5;
                case "scooter" -> 3.0;
                case "bike"    -> 2.5;
                default -> throw new IllegalArgumentException("Unsupported vehicle type: " + vehicleType);
            };
            case "pärnu", "parnu" -> switch (vehicleType.toLowerCase()) {
                case "car"     -> 3.0;
                case "scooter" -> 2.5;
                case "bike"    -> 2.0;
                default -> throw new IllegalArgumentException("Unsupported vehicle type: " + vehicleType);
            };
            default -> throw new IllegalArgumentException("Unsupported city: " + city);
        };
    }

    /**
     * It adds an extra fee if it is very cold. This only applies to scooters and bikes.
     */
    private double calculateAirTemperatureFee(String vehicleType, Double airTemperature) {
        if (airTemperature == null) {
            return 0.0;
        }
        String type = vehicleType.toLowerCase();
        if (!type.equals("scooter") && !type.equals("bike")) {
            return 0.0;
        }
        if (airTemperature < -10.0) {
            return 1.0;
        }
        if (airTemperature <= 0.0) {
            return 0.5;
        }
        return 0.0;
    }

    /**
     * It adds a fee if it is windy (only for bikes).
     * If the wind is too strong (over 20 m/s), it stops the delivery.
     */
    private double calculateWindSpeedFee(String vehicleType, Double windSpeed) {
        if (windSpeed == null) {
            return 0.0;
        }
        if (!vehicleType.equalsIgnoreCase("bike")) {
            return 0.0;
        }
        if (windSpeed > 20.0) {
            throw new ForbiddenVehicleException("Usage of selected vehicle type is forbidden");
        }
        if (windSpeed >= 10.0) {
            return 0.5;
        }
        return 0.0;
    }

    /**
     * It adds a fee for rain, snow, or sleet (for scooters and bikes).
     * It forbids delivery if there is thunder, hail, or glaze.
     */
    private double calculateWeatherPhenomenonFee(String vehicleType, String weatherPhenomenon) {
        if (weatherPhenomenon == null || weatherPhenomenon.isBlank()) {
            return 0.0;
        }
        String type = vehicleType.toLowerCase();
        if (!type.equals("scooter") && !type.equals("bike")) {
            return 0.0;
        }

        String phenomenon = weatherPhenomenon.toLowerCase();

        if (phenomenon.contains("glaze") || phenomenon.contains("hail") || phenomenon.contains("thunder")) {
            throw new ForbiddenVehicleException("Usage of selected vehicle type is forbidden");
        }
        if (phenomenon.contains("snow") || phenomenon.contains("sleet")) {
            return 1.0;
        }
        if (phenomenon.contains("rain")) {
            return 0.5;
        }
        return 0.0;
    }

    /**
     * This is the main method that calculates the final delivery price.
     * It gets the latest weather from the database and sums up all the fees.
     */
    public double calculateDeliveryFee(String city, String vehicleType) {
        log.debug("Calculating fee for city={}, vehicleType={}", city, vehicleType);

        String stationName = resolveStationName(city);

        WeatherObservation latest = weatherObservationRepository
                .findTopByStationNameOrderByObservationTimestampDesc(stationName)
                .orElseThrow(() -> new IllegalStateException(
                        "No weather data available for city: " + city));

        double rbf  = calculateRegionalBaseFee(city, vehicleType);
        double atef = calculateAirTemperatureFee(vehicleType, latest.getAirTemperature());
        double wsef = calculateWindSpeedFee(vehicleType, latest.getWindSpeed());
        double wpef = calculateWeatherPhenomenonFee(vehicleType, latest.getWeatherPhenomenon());

        double total = rbf + atef + wsef + wpef;
        log.debug("Fee breakdown: RBF={}, ATEF={}, WSEF={}, WPEF={}, total={}", rbf, atef, wsef, wpef, total);

        return total;
    }
}