package it.palestra.prenotazioni_palestra.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ControllerAuth {
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
