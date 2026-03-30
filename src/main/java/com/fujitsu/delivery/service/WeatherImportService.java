package com.fujitsu.delivery.service;

import com.fujitsu.delivery.entity.WeatherObservation;
import com.fujitsu.delivery.repository.WeatherObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This service handles getting weather data from the Estonian website.
 * It downloads the data, picks the right cities, and saves them to the database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherImportService {

    private static final String WEATHER_URL = "https://www.ilmateenistus.ee/ilma_andmed/xml/observations.php";

    /**
     * These are the three stations we need for the task.
     */
    private static final Set<String> TARGET_STATIONS = Set.of(
            "Tallinn-Harku",
            "Tartu-Tõravere",
            "Pärnu"
    );

    private final WeatherObservationRepository weatherObservationRepository;

    /**
     * It downloads the latest weather data from the Estonian Environment Agency XML feed.
     * It only picks out the data for Tallinn, Tartu, and Pärnu.
     * Then, it saves them as new rows in our database without deleting any old data.
     */
    public void importWeatherData() {
        log.info("Starting weather data import from {}", WEATHER_URL);
        try {
            String xml = fetchXml();
            List<WeatherObservation> observations = parseObservations(xml);
            weatherObservationRepository.saveAll(observations);
            log.info("Successfully imported {} weather observations", observations.size());
        } catch (Exception e) {
            log.error("Failed to import weather data", e);
        }
    }

    /**
     * It connects to the weather website and gets the raw XML text.
     */
    private String fetchXml() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(WEATHER_URL))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("Weather API responded with status {}", response.statusCode());
        return response.body();
    }

    /**
     * It reads the XML text and turns it into a list of weather objects for the database.
     */
    private List<WeatherObservation> parseObservations(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputStream inputStream = new java.io.ByteArrayInputStream(xml.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Document document = builder.parse(inputStream);
        document.getDocumentElement().normalize();

        // It gets the main timestamp from the XML to know when the weather was measured
        String timestampAttr = document.getDocumentElement().getAttribute("timestamp");
        LocalDateTime observationTimestamp = parseUnixTimestamp(timestampAttr);

        NodeList stations = document.getElementsByTagName("station");
        List<WeatherObservation> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        for (int i = 0; i < stations.getLength(); i++) {
            Element station = (Element) stations.item(i);
            String name = getTextContent(station, "name");

            if (!TARGET_STATIONS.contains(name)) {
                continue;
            }

            WeatherObservation obs = new WeatherObservation();
            obs.setStationName(name);
            obs.setWmoCode(getTextContent(station, "wmocode"));
            obs.setAirTemperature(parseDouble(getTextContent(station, "airtemperature")));
            obs.setWindSpeed(parseDouble(getTextContent(station, "windspeed")));
            obs.setWeatherPhenomenon(getTextContent(station, "phenomenon"));
            obs.setObservationTimestamp(observationTimestamp);
            obs.setImportedAt(now);

            result.add(obs);
            log.debug("Parsed observation for station: {}", name);
        }

        return result;
    }

    /**
     * It gets the text inside an XML tag. If the tag is missing, it returns an empty string.
     */
    private String getTextContent(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) {
            return "";
        }
        return nodes.item(0).getTextContent().trim();
    }

    /**
     * It turns a String into a number (Double). It returns null if the text is empty.
     */
    private Double parseDouble(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            log.warn("Could not parse numeric value: '{}'", value);
            return null;
        }
    }

    /**
     * It converts the long number of seconds from the XML into a normal Java date and time.
     */
    private LocalDateTime parseUnixTimestamp(String value) {
        try {
            long epoch = Long.parseLong(value);
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneOffset.UTC);
        } catch (NumberFormatException e) {
            log.warn("Could not parse timestamp '{}', using current time", value);
            return LocalDateTime.now(ZoneOffset.UTC);
        }
    }
}