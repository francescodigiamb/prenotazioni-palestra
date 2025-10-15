package it.palestra.prenotazioni_palestra.config;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Es: "Corso non trovato: 999" (tu già lanci IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleIllegalArgument(IllegalArgumentException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", ex.getMessage());
        return "error-404";
    }

    // (opzionale) eventuali altri 404 applicativi
    @ExceptionHandler(java.util.NoSuchElementException.class)
    public String handleNoSuchElement(java.util.NoSuchElementException ex, Model model) {
        model.addAttribute("status", 404);
        model.addAttribute("message", "Risorsa non trovata.");
        return "error-404";
    }
}
