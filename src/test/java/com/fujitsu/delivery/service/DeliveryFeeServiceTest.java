package com.fujitsu.delivery.service;

import com.fujitsu.delivery.entity.WeatherObservation;
import com.fujitsu.delivery.exception.ForbiddenVehicleException;
import com.fujitsu.delivery.repository.WeatherObservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * This class tests the DeliveryFeeService to make sure all fee rules work.
 * It uses Mockito to simulate the database so it can test different weather scenarios.
 */
@ExtendWith(MockitoExtension.class)
class DeliveryFeeServiceTest {

    @Mock
    private WeatherObservationRepository weatherObservationRepository;

    @InjectMocks
    private DeliveryFeeService deliveryFeeService;

    private WeatherObservation tartuObservation;

    /**
     * It sets up a default weather report for Tartu before every test.
     */
    @BeforeEach
    void setUp() {
        tartuObservation = new WeatherObservation();
        tartuObservation.setStationName("Tartu-Tõravere");
        tartuObservation.setWmoCode("26242");
        tartuObservation.setObservationTimestamp(LocalDateTime.now());
        tartuObservation.setImportedAt(LocalDateTime.now());
    }

    // --- Example from Fujitsu ---

    /**
     * It tests the exact example provided in the project instructions.
     */
    @Test
    void shouldCalculateExampleFromSpec() {
        // Tartu + Bike, temp=-2.1, wind=4.7, phenomenon="Light snow shower"
        // Expected: 2.5 (base) + 0.5 (temp) + 0 (wind) + 1 (snow) = 4.0
        tartuObservation.setAirTemperature(-2.1);
        tartuObservation.setWindSpeed(4.7);
        tartuObservation.setWeatherPhenomenon("Light snow shower");

        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));

        double fee = deliveryFeeService.calculateDeliveryFee("tartu", "bike");
        assertThat(fee).isEqualTo(4.0);
    }

    // --- Regional base fee tests ---

    /**
     * It checks the base price for a car in Tallinn (4.0).
     */
    @Test
    void shouldReturnCorrectRbfForTallinnCar() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("tallinn", "car")).isEqualTo(4.0);
    }

    /**
     * It checks the base price for a scooter in Tallinn (3.5).
     */
    @Test
    void shouldReturnCorrectRbfForTallinnScooter() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("tallinn", "scooter")).isEqualTo(3.5);
    }

    /**
     * It checks the base price for a bike in Pärnu (2.0).
     */
    @Test
    void shouldReturnCorrectRbfForParnuBike() {
        WeatherObservation obs = neutralObservation("Pärnu");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Pärnu"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("parnu", "bike")).isEqualTo(2.0);
    }

    // --- Air temperature extra fee ---

    /**
     * It checks that bikes get a 1 euro extra fee when it's very cold (-15).
     */
    @Test
    void shouldAddAtef1EuroWhenTempBelow10ForBike() {
        tartuObservation.setAirTemperature(-15.0);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.5); // RBF=2.5, ATEF=1
    }

    /**
     * It ensures cars never charge extra depending on temperature.
     */
    @Test
    void shouldNotApplyAtef_ForCar() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        obs.setAirTemperature(-20.0);
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("tallinn", "car")).isEqualTo(4.0);
    }

    // --- Wind speed extra fee ---

    /**
     * It checks if it's 0.50 extra when the wind is 15 m/s for bike.
     */
    @Test
    void shouldAddWsef05WhenWindBetween10And20ForBike() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(15.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.0); // RBF=2.5, WSEF=0.5
    }

    /**
     * It checks that bike delivery is forbidden if the wind is over 20 m/s.
     */
    @Test
    void shouldThrowForbiddenWhenWindOver20ForBike() {
        tartuObservation.setAirTemperature(5.0);
        tartuObservation.setWindSpeed(25.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));

        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("tartu", "bike"))
                .isInstanceOf(ForbiddenVehicleException.class)
                .hasMessageContaining("forbidden");
    }

    // --- Weather phenomenon extra fee ---

    /**
     * It checks if snow adds 1 euro to the scooter fee.
     */
    @Test
    void shouldAddWpef1EuroForSnowOnScooter() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        obs.setWeatherPhenomenon("Heavy snow");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        // RBF=3.5, WPEF=1
        assertThat(deliveryFeeService.calculateDeliveryFee("tallinn", "scooter")).isEqualTo(4.5);
    }

    /**
     * It checks if rain adds 0.50 euros to the bike fee.
     */
    @Test
    void shouldAddWpef05EuroForRainOnBike() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("Light rain");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        // RBF=2.5, WPEF=0.5
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.0);
    }

    /**
     * It checks that glaze phenomenon forbids scooter delivery.
     */
    @Test
    void shouldThrowForbiddenWhenGlazeForScooter() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        obs.setWeatherPhenomenon("Glaze");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));

        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("tallinn", "scooter"))
                .isInstanceOf(ForbiddenVehicleException.class)
                .hasMessageContaining("forbidden");
    }

    /**
     * It checks that thunder forbids bike delivery.
     */
    @Test
    void shouldThrowForbiddenWhenThunderForBike() {
        tartuObservation.setAirTemperature(15.0);
        tartuObservation.setWindSpeed(5.0);
        tartuObservation.setWeatherPhenomenon("Thunderstorm");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));

        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("tartu", "bike"))
                .isInstanceOf(ForbiddenVehicleException.class);
    }

    // --- Error cases ---

    /**
     * It makes sure an error is thrown for an unknown city.
     */
    @Test
    void shouldThrowIllegalArgumentForUnsupportedCity() {
        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("Helsinki", "car"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * It makes sure an error is thrown for an unknown vehicle.
     */
    @Test
    void shouldThrowIllegalArgumentForUnsupportedVehicle() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));

        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("tallinn", "helicopter"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * It makes sure an error is thrown if there is no data in the database.
     */
    @Test
    void shouldThrowIllegalStateWhenNoWeatherData() {
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("tallinn", "car"))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- Boundary: air temperature exactly at -10 or 0 ---

    /**
     * It checks the fee when the temperature is exactly -10.0.
     */
    @Test
    void shouldAddAtef1Euro_WhenTempExactlyMinus10() {
        tartuObservation.setAirTemperature(-10.0);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.0);
    }

    /**
     * It checks the fee when the temperature is just below -10.
     */
    @Test
    void shouldAddAtef1Euro_WhenTempJustBelow10() {
        tartuObservation.setAirTemperature(-10.1);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.5);
    }

    /**
     * It checks the fee when the temperature is exactly 0.0.
     */
    @Test
    void shouldAddAtef05Euro_WhenTempExactly0() {
        tartuObservation.setAirTemperature(0.0);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.0);
    }

    /**
     * It checks that no extra fee is added when it is just above 0.
     */
    @Test
    void shouldNotAddAtef_WhenTempJustAbove0() {
        tartuObservation.setAirTemperature(0.1);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(2.5);
    }

    // --- Boundary: wind speed exactly at 10 or 20 ---

    /**
     * It checks the fee when the wind is exactly 10.0.
     */
    @Test
    void shouldAddWsef05_WhenWindExactly10() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(10.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.0);
    }

    /**
     * It checks the fee when the wind is exactly 20.0.
     */
    @Test
    void shouldAddWsef05_WhenWindExactly20() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(20.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.0);
    }

    /**
     * It checks that bike delivery is forbidden when wind is just over 20.
     */
    @Test
    void shouldThrowForbidden_WhenWindJustOver20ForBike() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(20.1);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));

        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("tartu", "bike"))
                .isInstanceOf(ForbiddenVehicleException.class);
    }

    // --- Scooter is not affected by wind at all ---

    /**
     * It ensures scooters can still deliver even in high wind.
     */
    @Test
    void shouldNotApplyWsef_ForScooterEvenWithHighWind() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        obs.setWindSpeed(25.0);
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("tallinn", "scooter")).isEqualTo(3.5);
    }

    // --- Sleet phenomenon ---

    /**
     * It checks that sleet adds a 1 euro fee for bikes.
     */
    @Test
    void shouldAddWpef1Euro_ForSleetOnBike() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("Light sleet");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(3.5);
    }

    // --- Hail forbidden ---

    /**
     * It checks that hail forbids bike delivery.
     */
    @Test
    void shouldThrowForbidden_WhenHailForBike() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("Hail");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));

        assertThatThrownBy(() -> deliveryFeeService.calculateDeliveryFee("tartu", "bike"))
                .isInstanceOf(ForbiddenVehicleException.class)
                .hasMessageContaining("forbidden");
    }

    // --- Car is never affected by weather extra fees ---

    /**
     * It ensures cars never pay extra fees regardless of how bad the weather is.
     */
    @Test
    void shouldNotApplyAnyExtraFees_ForCarInBadWeather() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        obs.setAirTemperature(-20.0);
        obs.setWindSpeed(30.0);
        obs.setWeatherPhenomenon("Heavy snow shower");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("tallinn", "car")).isEqualTo(4.0);
    }

    // --- Case insensitivity ---

    /**
     * It ensures the app works if the user types in ALL CAPS.
     */
    @Test
    void shouldHandleUpperCaseCityAndVehicle() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("TALLINN", "CAR")).isEqualTo(4.0);
    }

    /**
     * It ensures the app works if the user types with Mixed Case.
     */
    @Test
    void shouldHandleMixedCaseCityAndVehicle() {
        WeatherObservation obs = neutralObservation("Tallinn-Harku");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tallinn-Harku"))
                .thenReturn(Optional.of(obs));
        assertThat(deliveryFeeService.calculateDeliveryFee("Tallinn", "Scooter")).isEqualTo(3.5);
    }

    // --- Null weather values (station reported no data) ---

    /**
     * It makes sure the app doesn't crash if the temperature data is missing.
     */
    @Test
    void shouldNotCrash_WhenAirTemperatureIsNull() {
        tartuObservation.setAirTemperature(null);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(2.5);
    }

    /**
     * It makes sure the app doesn't crash if the wind speed data is missing.
     */
    @Test
    void shouldNotCrash_WhenWindSpeedIsNull() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(null);
        tartuObservation.setWeatherPhenomenon("");
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(2.5);
    }

    /**
     * It makes sure the app doesn't crash if the weather phenomenon data is missing.
     */
    @Test
    void shouldNotCrash_WhenWeatherPhenomenonIsNull() {
        tartuObservation.setAirTemperature(10.0);
        tartuObservation.setWindSpeed(0.0);
        tartuObservation.setWeatherPhenomenon(null);
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc("Tartu-Tõravere"))
                .thenReturn(Optional.of(tartuObservation));
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "bike")).isEqualTo(2.5);
    }

    // --- All three cities base fee check ---

    /**
     * It checks the base prices for cars across all three cities.
     */
    @Test
    void shouldReturnCorrectRbf_ForAllCitiesWithCar() {
        mockStation("Tallinn-Harku", neutralObservation("Tallinn-Harku"));
        mockStation("Tartu-Tõravere", neutralObservation("Tartu-Tõravere"));
        mockStation("Pärnu", neutralObservation("Pärnu"));

        assertThat(deliveryFeeService.calculateDeliveryFee("tallinn", "car")).isEqualTo(4.0);
        assertThat(deliveryFeeService.calculateDeliveryFee("tartu", "car")).isEqualTo(3.5);
        assertThat(deliveryFeeService.calculateDeliveryFee("parnu", "car")).isEqualTo(3.0);
    }

    // --- Helper ---

    /**
     * A helper method to create "normal" weather where no extra fees are added.
     */
    private WeatherObservation neutralObservation(String stationName) {
        WeatherObservation obs = new WeatherObservation();
        obs.setStationName(stationName);
        obs.setWmoCode("00000");
        obs.setAirTemperature(15.0);
        obs.setWindSpeed(3.0);
        obs.setWeatherPhenomenon("");
        obs.setObservationTimestamp(LocalDateTime.now());
        obs.setImportedAt(LocalDateTime.now());
        return obs;
    }

    /**
     * A helper method to mock a specific station in the repository.
     */
    private void mockStation(String stationName, WeatherObservation obs) {
        when(weatherObservationRepository.findTopByStationNameOrderByObservationTimestampDesc(stationName))
                .thenReturn(Optional.of(obs));
    }
}