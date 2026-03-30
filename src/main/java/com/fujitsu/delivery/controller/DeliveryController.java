package com.fujitsu.delivery.controller;

import com.fujitsu.delivery.dto.DeliveryResponse;
import com.fujitsu.delivery.service.DeliveryFeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is the controller that creates the web link (API) for the app.
 * The main address is /api/delivery.
 * If there is an error, the GlobalExceptionHandler will handle it.
 */
@RestController
@RequestMapping("/api/delivery")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryFeeService deliveryFeeService;

    /**
     * This method handles the request when someone goes to /api/delivery/fee.
     * It takes the city and vehicle from the URL, gets the fee from the service,
     * and returns the result as a JSON response.
     * Example: /api/delivery/fee?city=Tallinn&vehicleType=Bike
     */
    @GetMapping("/fee")
    public ResponseEntity<DeliveryResponse> getDeliveryFee(
            @RequestParam String city,
            @RequestParam String vehicleType) {

        double fee = deliveryFeeService.calculateDeliveryFee(city, vehicleType);
        return ResponseEntity.ok(new DeliveryResponse(city, vehicleType, fee));
    }
}