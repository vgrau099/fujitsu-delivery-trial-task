package com.fujitsu.delivery.exception;

/**
 * This error is used when the weather is too dangerous for a certain vehicle.
 * For example, if it's too windy for a bike or if there is thunder.
 */
public class ForbiddenVehicleException extends RuntimeException {

    public ForbiddenVehicleException(String message) {
        super(message);
    }
}