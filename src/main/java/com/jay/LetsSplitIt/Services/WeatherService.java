package com.jay.LetsSplitIt.Services;

import com.jay.LetsSplitIt.Dto.WeatherDto;
import com.jay.LetsSplitIt.Dto.WeatherStackResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;

@Service
public class WeatherService {

    private final RestClient weatherRestClient;
    private final String accessKey;

    public WeatherService(
            RestClient weatherRestClient,
            @Value("${weatherstack.access-key}") String accessKey) {
        this.weatherRestClient = weatherRestClient;
        this.accessKey = accessKey;
    }

    public WeatherDto getByCoordinates(double lat, double lon) {
        return fetch(lat + "," + lon);
    }

    public WeatherDto getByCity(String city) {
        return fetch(city);
    }

    private WeatherDto fetch(String query) {
        WeatherStackResponse response = weatherRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/current")
                        .queryParam("query", query)
                        .queryParam("access_key", accessKey)
                        .build())
                .retrieve()
                .body(WeatherStackResponse.class);

        if (response == null || response.current() == null || response.location() == null) {
            String info = response != null && response.error() != null
                    ? response.error().info() : "empty response";
            throw new ResponseStatusException(BAD_GATEWAY, "Weather lookup failed: " + info);
        }

        var c = response.current();
        var l = response.location();
        return new WeatherDto(
                l.name(),
                l.country(),
                l.localtime(),
                c.temperature(),
                c.feelslike(),
                c.humidity(),
                c.wind_speed(),
                c.wind_dir(),
                c.weather_descriptions() == null || c.weather_descriptions().isEmpty()
                        ? null : c.weather_descriptions().get(0),
                c.weather_icons() == null || c.weather_icons().isEmpty()
                        ? null : c.weather_icons().get(0));
    }
}
