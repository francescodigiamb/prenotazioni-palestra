package it.palestra.prenotazioni_palestra.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;

@Controller
public class ControllerCorsi {
    private final CorsoRepository corsoRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    // metodo per vedere se la tabella è da eliminare o meno in base alla data
    private boolean isExpired(Corso c) {
        LocalDate today = LocalDate.now();
        if (c.getData().isBefore(today))
            return true;
        if (c.getData().isEqual(today) && c.getOrario().isBefore(LocalTime.now()))
            return true;
        return false;
    }

    public ControllerCorsi(CorsoRepository corsoRepository, PrenotazioneRepository prenotazioneRepository) {
        this.corsoRepository = corsoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    @GetMapping("/corsi")
    public String listaCorsi(Model model) {
        List<Corso> corsi;
        try {
            corsi = corsoRepository.findAll();
        } catch (Exception e) {
            corsi = Collections.emptyList();
        }

        // Filtro fuori i corsi scaduti
        List<Corso> visibili = corsi.stream()
                .filter(c -> !isExpired(c))
                .toList();

        // Calcoli per disponibilità sui corsi visibili
        Map<Integer, Integer> prenotatiMap = new HashMap<>();
        Map<Integer, Integer> disponibiliMap = new HashMap<>();
        Map<Integer, Boolean> pienoMap = new HashMap<>();

        for (Corso c : visibili) {
            int pren = prenotazioneRepository.countByCorso(c);
            int disp = Math.max(c.getMaxPosti() - pren, 0);
            prenotatiMap.put(c.getId(), pren);
            disponibiliMap.put(c.getId(), disp);
            pienoMap.put(c.getId(), disp == 0);
        }

        model.addAttribute("corsi", visibili);
        model.addAttribute("prenotatiMap", prenotatiMap);
        model.addAttribute("disponibiliMap", disponibiliMap);
        model.addAttribute("pienoMap", pienoMap);
        return "corsi"; // templates/corsi.html
    }

    // GET /corsi/{id} -> dettaglio corso con posti disponibili
    @GetMapping("/corsi/{id}")
    public String dettaglioCorso(@PathVariable Integer id, Model model) {

        // 1) Carico il corso dall'id
        Optional<Corso> nomeCorso = corsoRepository.findById(id);
        if (!nomeCorso.isPresent()) {

            throw new IllegalArgumentException("Corso non trovato: " + id);
        }
        Corso corso = nomeCorso.get();

        // 2) Conteggio prenotazioni per questo corso
        int prenotati = prenotazioneRepository.countByCorso(corso);
        // 3) Calcolo posti disponibili (non sotto zero)
        int postiTotali = corso.getMaxPosti();
        int postiDisponibili = postiTotali - prenotati;
        if (postiDisponibili < 0) {
            postiDisponibili = 0;
        }
        // 🔽 ORDINATO PER DATA (più recente in alto)
        List<Prenotazione> prenotazioniCorso = prenotazioneRepository.findByCorsoOrderByCreatedAtDesc(corso);

        // Per badge “Nuovo” nelle ultime 24h
        LocalDateTime nowMinus24h = LocalDateTime.now().minusHours(24);
        // 4) Aggiungo attributi al model per Thymeleaf
        model.addAttribute("corso", corso);
        model.addAttribute("prenotati", prenotati);
        model.addAttribute("postiDisponibili", postiDisponibili);
        model.addAttribute("prenotazioniCorso", prenotazioniCorso);
        model.addAttribute("nowMinus24h", nowMinus24h);

        return "corso-dettaglio";
    }

    // pagina per il form di prenotazione corso
    @GetMapping("/corsi/{id}/prenota")
    public String prenotaCorso(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        Optional<Corso> maybeCorso = corsoRepository.findById(id);
        if (!maybeCorso.isPresent())
            throw new IllegalArgumentException("Corso non trovato: " + id);
        Corso corso = maybeCorso.get();

        // ⛔ Blocca se scaduto
        if (isExpired(corso)) {
            ra.addFlashAttribute("error", "Il corso è scaduto: non è più possibile prenotare.");
            return "redirect:/corsi/" + id;
        }

        int prenotati = prenotazioneRepository.countByCorso(corso);
        int postiDisponibili = Math.max(corso.getMaxPosti() - prenotati, 0);

        model.addAttribute("corso", corso);
        model.addAttribute("prenotati", prenotati);
        model.addAttribute("postiDisponibili", postiDisponibili);
        return "prenota-corso";
    }

}
