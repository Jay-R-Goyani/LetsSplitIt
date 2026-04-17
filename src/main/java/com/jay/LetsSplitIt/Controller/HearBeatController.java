package com.jay.LetsSplitIt.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HearBeatController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello World";
    }
}
