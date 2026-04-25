package com.jay.LetsSplitIt.Dto;

public record WeatherDto(
        String city,
        String country,
        String localTime,
        int temperatureCelsius,
        int feelsLikeCelsius,
        int humidity,
        int windSpeedKmh,
        String windDirection,
        String description,
        String iconUrl) {}
