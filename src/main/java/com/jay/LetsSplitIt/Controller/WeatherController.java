package com.jay.LetsSplitIt.Controller;

import com.jay.LetsSplitIt.Dto.WeatherDto;
import com.jay.LetsSplitIt.Services.WeatherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/weather")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/current")
    public WeatherDto current(
            @RequestParam double lat,
            @RequestParam double lon) {
        return weatherService.getByCoordinates(lat, lon);
    }

    @GetMapping("/city")
    public WeatherDto byCity(@RequestParam String name) {
        return weatherService.getByCity(name);
    }
}
