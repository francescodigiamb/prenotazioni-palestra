package it.palestra.prenotazioni_palestra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControllerPagine {
    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }

    @GetMapping("/cookie")
    public String cookie() {
        return "cookie";
    }
}
