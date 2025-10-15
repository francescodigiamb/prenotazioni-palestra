package it.palestra.prenotazioni_palestra.config;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

@ControllerAdvice
public class WebBindingConfig {

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        // Con true: converte stringhe vuote ("") in null
        binder.registerCustomEditor(String.class, new StringTrimmerEditor(true));
    }
}
