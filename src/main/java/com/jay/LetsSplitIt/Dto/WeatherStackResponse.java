package com.jay.LetsSplitIt.Dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeatherStackResponse(
        Location location,
        Current current,
        ErrorBody error) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(
            String name,
            String country,
            String region,
            String localtime) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Current(
            Integer temperature,
            Integer feelslike,
            Integer humidity,
            Integer wind_speed,
            String wind_dir,
            Integer uv_index,
            Integer visibility,
            List<String> weather_descriptions,
            List<String> weather_icons) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorBody(Integer code, String type, String info) {}
}
