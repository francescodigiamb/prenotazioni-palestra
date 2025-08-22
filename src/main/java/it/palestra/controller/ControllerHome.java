package it.palestra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControllerHome {
    @GetMapping("/")
    public String home() {
        return "home"; // carica il file templates/home.html
    }
}
