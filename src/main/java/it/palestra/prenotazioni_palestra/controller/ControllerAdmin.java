package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class ControllerAdmin {

    private final CorsoRepository corsoRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    public ControllerAdmin(CorsoRepository corsoRepository, PrenotazioneRepository prenotazioneRepository) {
        this.corsoRepository = corsoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    private boolean isExpired(Corso c) {
        LocalDate today = LocalDate.now();
        if (c.getData().isBefore(today))
            return true;
        return c.getData().isEqual(today) && c.getOrario().isBefore(LocalTime.now());
    }

    @GetMapping("/corsi-archiviati")
    public String corsiArchiviati(Model model) {
        List<Corso> tutti = corsoRepository.findAll();

        List<Corso> archiviati = tutti.stream()
                .filter(this::isExpired)
                .sorted(Comparator
                        .comparing(Corso::getData).reversed()
                        .thenComparing(Corso::getOrario).reversed())
                .toList();

        Map<Integer, Integer> prenotatiMap = new HashMap<>();
        for (Corso c : archiviati) {
            prenotatiMap.put(c.getId(), prenotazioneRepository.countByCorso(c));
        }

        model.addAttribute("corsi", archiviati);
        model.addAttribute("prenotatiMap", prenotatiMap);
        return "admin-corsi-archiviati";
    }
}
