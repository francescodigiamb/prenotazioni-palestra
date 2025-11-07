package it.palestra.prenotazioni_palestra.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
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
    // private boolean isExpired(Corso c) {
    // LocalDate today = LocalDate.now();
    // if (c.getData().isBefore(today))
    // return true;
    // if (c.getData().isEqual(today) && c.getOrario().isBefore(LocalTime.now()))
    // return true;
    // return false;
    // }

    public ControllerCorsi(CorsoRepository corsoRepository, PrenotazioneRepository prenotazioneRepository) {
        this.corsoRepository = corsoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    @GetMapping("/corsi")
    public String listaCorsi(
            @RequestParam(value = "nome", required = false) String nome,
            Model model) {

        List<Corso> tutti;
        try {
            tutti = corsoRepository.findAll();
        } catch (Exception e) {
            tutti = Collections.emptyList();
        }

        LocalDate oggi = LocalDate.now();
        LocalTime ora = LocalTime.now();
        LocalDate limite = oggi.plusWeeks(2); // 14 giorni da oggi

        // Filtra: non scaduti e entro 14 giorni
        List<Corso> futuri = tutti.stream()
                .filter(c -> {
                    LocalDate d = c.getData();
                    LocalTime t = c.getOrario();
                    boolean nonScaduto = d.isAfter(oggi) ||
                            (d.isEqual(oggi) && t.isAfter(ora));
                    boolean entroLimite = !d.isAfter(limite);
                    return nonScaduto && entroLimite;
                })
                .toList();

        // Filtro per nome corso (se arrivi da /catalogo?nome=Pilates)
        if (nome != null && !nome.isBlank()) {
            futuri = futuri.stream()
                    .filter(c -> c.getNome() != null && c.getNome().equalsIgnoreCase(nome.trim()))
                    .toList();
            model.addAttribute("filtroNome", nome.trim());
        }

        // Mappe per numero prenotati / corso pieno
        var prenotatiMap = new java.util.HashMap<Integer, Integer>();
        var pienoMap = new java.util.HashMap<Integer, Boolean>();

        for (Corso c : futuri) {
            int pren = prenotazioneRepository.countByCorso(c);
            prenotatiMap.put(c.getId(), pren);
            pienoMap.put(c.getId(), pren >= c.getMaxPosti());
        }

        model.addAttribute("corsi", futuri);
        model.addAttribute("prenotatiMap", prenotatiMap);
        model.addAttribute("pienoMap", pienoMap);
        model.addAttribute("maxGiorniPrenotazione", 14);

        return "corsi";
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
    public String prenotaCorso(@PathVariable Integer id,
            Model model,
            RedirectAttributes ra) {

        Optional<Corso> maybeCorso = corsoRepository.findById(id);
        if (!maybeCorso.isPresent()) {
            ra.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/corsi";
        }
        Corso corso = maybeCorso.get();

        LocalDate oggi = LocalDate.now();
        LocalDate limite = oggi.plusWeeks(2);

        // Se il corso è oltre la finestra di prenotazione → blocca
        if (corso.getData().isAfter(limite)) {
            ra.addFlashAttribute("warning",
                    "Puoi prenotare questo corso solo a partire da 14 giorni prima della data.");
            return "redirect:/corsi";
        }

        // Calcolo posti disponibili
        int prenotati = prenotazioneRepository.countByCorso(corso);
        int postiDisponibili = corso.getMaxPosti() - prenotati;
        if (postiDisponibili < 0)
            postiDisponibili = 0;

        model.addAttribute("corso", corso);
        model.addAttribute("prenotati", prenotati);
        model.addAttribute("postiDisponibili", postiDisponibili);

        return "prenota-corso";
    }

    // helper per nome giorno in IT
    private static String giornoIt(java.time.DayOfWeek dow) {
        switch (dow) {
            case MONDAY:
                return "Lunedì";
            case TUESDAY:
                return "Martedì";
            case WEDNESDAY:
                return "Mercoledì";
            case THURSDAY:
                return "Giovedì";
            case FRIDAY:
                return "Venerdì";
            case SATURDAY:
                return "Sabato";
            case SUNDAY:
                return "Domenica";
        }
        return dow.name();
    }

    // DTO view per il catalogo
    public static class SchedaCatalogo {
        public String nome;
        public String descrizione;
        public java.util.List<String> slot = new java.util.ArrayList<>(); // es: "Martedì 19:00"
        public Integer capienzaTipica; // maxPosti più frequente tra le future
    }

    @GetMapping("/catalogo")
    public String catalogoCorsi(Model model) {
        java.time.LocalDate oggi = java.time.LocalDate.now();

        // Prendiamo SOLO occorrenze future/scadute oggi ma non ancora iniziate
        java.util.List<it.palestra.prenotazioni_palestra.model.Corso> occorrenze = corsoRepository.findAll().stream()
                .filter(c -> c.getData().isAfter(oggi) ||
                        (c.getData().isEqual(oggi) && c.getOrario().isAfter(java.time.LocalTime.now())))
                .toList();

        // Raggruppa per nome corso
        java.util.Map<String, java.util.List<it.palestra.prenotazioni_palestra.model.Corso>> byNome = occorrenze
                .stream().collect(
                        java.util.stream.Collectors.groupingBy(it.palestra.prenotazioni_palestra.model.Corso::getNome));

        java.util.List<SchedaCatalogo> schede = new java.util.ArrayList<>();
        for (var entry : byNome.entrySet()) {
            String nome = entry.getKey();
            var list = entry.getValue();

            // descrizione: prendi la prima non vuota
            String descr = list.stream()
                    .map(it.palestra.prenotazioni_palestra.model.Corso::getDescrizione)
                    .filter(s -> s != null && !s.isBlank())
                    .findFirst().orElse("");

            // slot unici: giorno della settimana + orario (ordinati)
            java.util.Set<String> slotSet = new java.util.TreeSet<>();
            for (var c : list) {
                String s = giornoIt(c.getData().getDayOfWeek()) + " " +
                        c.getOrario().toString().substring(0, 5); // HH:mm
                slotSet.add(s);
            }

            // capienza tipica: moda dei maxPosti
            java.util.Map<Integer, Long> countByCap = list.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            it.palestra.prenotazioni_palestra.model.Corso::getMaxPosti,
                            java.util.stream.Collectors.counting()));
            Integer capTipica = countByCap.entrySet().stream()
                    .max(java.util.Map.Entry.comparingByValue())
                    .map(java.util.Map.Entry::getKey).orElse(null);

            SchedaCatalogo s = new SchedaCatalogo();
            s.nome = nome;
            s.descrizione = descr;
            s.capienzaTipica = capTipica;
            s.slot = new java.util.ArrayList<>(slotSet);
            schede.add(s);
        }

        // Ordina alfabetico per nome
        schede.sort(java.util.Comparator.comparing(sc -> sc.nome.toLowerCase()));

        model.addAttribute("schede", schede);
        return "catalogo-corsi";
    }

}
