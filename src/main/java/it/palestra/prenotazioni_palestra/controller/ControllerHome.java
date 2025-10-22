package it.palestra.prenotazioni_palestra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControllerHome {
    @GetMapping({ "/", "/home" })
    public String home() {
        return "home"; // carica il file templates/home.html
    }

    @GetMapping("/listino")
    public String listino() {
        return "listino"; // templates/listino.html
    }
}
