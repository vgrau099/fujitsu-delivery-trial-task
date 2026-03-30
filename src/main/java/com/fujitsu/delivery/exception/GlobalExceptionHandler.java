package com.fujitsu.delivery.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Error Handling for REST with Spring: Used as a reference for creating the GlobalExceptionHandler.
 *
 * URL: https://www.baeldung.com/exception-handling-for-rest-with-spring
 */

/**
 * This class catches errors that happen anywhere in the app and
 * sends back a message to the user.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * It handles cases where a vehicle is forbidden due to bad weather.
     */
    @ExceptionHandler(ForbiddenVehicleException.class)
    public ResponseEntity<Map<String, String>> handleForbiddenVehicle(ForbiddenVehicleException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * It handles cases where we try to calculate a fee but have no weather data yet.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", ex.getMessage()));
    }

    /**
     * It handles cases where the user types a city or vehicle that doesn't exist (on adress bar).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }
}