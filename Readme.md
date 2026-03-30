# Delivery Fee Calculator

A Spring Boot REST application that calculates food delivery fees based on city, vehicle type, and live weather conditions from the Estonian Environment Agency.

## Requirements

- Java 21
- Maven

## Running the app

Open the project in IntelliJ IDEA and run `DeliveryApplication.java`, or from the terminal: `mvn spring-boot:run`

On startup, weather data is fetched automatically. The app then refreshes it every hour at HH:15:00. The server runs on `http://localhost:8080`.

## Using the API

Accepted values:
- `city`: Tallinn, Tartu, Parnu
- `vehicleType`: Car, Scooter, Bike

Example requests:
```
http://localhost:8080/api/delivery/fee?city=Tallinn&vehicleType=Bike
http://localhost:8080/api/delivery/fee?city=Tartu&vehicleType=Scooter
http://localhost:8080/api/delivery/fee?city=Parnu&vehicleType=Car
```

Example response:
```json
{"city": "Tallinn", "vehicleType": "Bike", "totalFee": 3.5}
```

If weather conditions forbid the vehicle type:
```json
{"error": "Usage of selected vehicle type is forbidden"}
```

## Running the tests

Run `DeliveryFeeServiceTest.java` in IntelliJ, or from the terminal: `mvn test`


## Viewing the database

The H2 console is at `http://localhost:8080/h2-console` while the app is running.
- JDBC URL: `jdbc:h2:mem:deliverydb`
- Username: `SA`
- Password: (leave blank)

Run `SELECT * FROM WEATHER_OBSERVATION;` to inspect stored weather data.