package it.palestra.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;

@Controller
public class ControllerCorsi {
    private final CorsoRepository corsoRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    public ControllerCorsi(CorsoRepository corsoRepository, PrenotazioneRepository prenotazioneRepository) {
        this.corsoRepository = corsoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    @GetMapping("/corsi")
    public String listaCorsi(Model model) {
        List<Corso> corsi = corsoRepository.findAll();
        model.addAttribute("corsi", corsi);
        return "corsi"; // templates/corsi.html
    }

    // GET /corsi/{id} -> dettaglio corso con posti disponibili
    @GetMapping("/corsi/{id}")
    public String dettaglioCorso(@PathVariable Integer id, Model model) {

        // 1) Carico il corso dall'id (senza lambda)
        Optional<Corso> nomeCorso = corsoRepository.findById(id);
        if (!nomeCorso.isPresent()) {

            throw new IllegalArgumentException("Corso non trovato: " + id);
        }
        Corso corso = nomeCorso.get();

        // 2) Conteggio prenotazioni per questo corso
        // (versione "ad oggetto": passo direttamente l'entity Corso)
        int prenotati = prenotazioneRepository.countByCorso(corso);

        // 3) Calcolo posti disponibili (non sotto zero)
        int postiTotali = corso.getMaxPosti();
        int postiDisponibili = postiTotali - prenotati;
        if (postiDisponibili < 0) {
            postiDisponibili = 0;
        }

        // 4) Aggiungo attributi al model per Thymeleaf
        model.addAttribute("corso", corso);
        model.addAttribute("prenotati", prenotati);
        model.addAttribute("postiDisponibili", postiDisponibili);

        return "corso-dettaglio";
    }

}
