package com.fujitsu.delivery.dto;

/**
 * This is a simple object used to send the result back to the browser.
 * It holds the city, the vehicle, and the final calculated price.
 */
public record DeliveryResponse(String city, String vehicleType, double totalFee) {
}