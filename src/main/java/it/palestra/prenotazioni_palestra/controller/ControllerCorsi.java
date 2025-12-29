package it.palestra.prenotazioni_palestra.controller;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
            @RequestParam(value = "q", required = false) String q,
            Model model) {

        List<Corso> tutti;
        try {
            tutti = corsoRepository.findAll();
        } catch (Exception e) {
            tutti = Collections.emptyList();
        }

        LocalDate oggi = LocalDate.now();
        LocalTime ora = LocalTime.now();
        LocalDate limite = oggi.plusWeeks(2); // max 14 giorni

        // 1) filtro base: non scaduti + entro 14 giorni
        List<Corso> futuri = tutti.stream()
                .filter(c -> {
                    LocalDate d = c.getData();
                    LocalTime t = c.getOrario();
                    boolean nonScaduto = d.isAfter(oggi) || (d.isEqual(oggi) && t.isAfter(ora));
                    boolean entroLimite = !d.isAfter(limite);
                    return nonScaduto && entroLimite;
                })
                .toList();

        // 2) filtro di ricerca (se q presente)
        String query = (q != null) ? q.trim().toLowerCase() : null;
        if (query != null && !query.isEmpty()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            futuri = futuri.stream()
                    .filter(c -> {
                        // nome corso
                        boolean byName = c.getNome() != null &&
                                c.getNome().toLowerCase().contains(query);

                        // giorno settimana in italiano (lun, martedi, ecc.)
                        String giornoIt = giornoSettimanaItaliano(c.getData().getDayOfWeek()).toLowerCase();
                        boolean byGiorno = giornoIt.startsWith(query);

                        // data formattata dd/MM/yyyy
                        String dataStr = c.getData().format(fmt);
                        boolean byData = dataStr.contains(query);

                        return byName || byGiorno || byData;
                    })
                    .toList();

            model.addAttribute("q", q);
        }

        // mappe prenotati / stato capienza
        var prenotatiMap = new java.util.HashMap<Integer, Integer>(); // totale (normali + riserva)
        var prenotatiNormaliMap = new java.util.HashMap<Integer, Integer>();
        var soloRiservaMap = new java.util.HashMap<Integer, Boolean>();
        var pienoMap = new java.util.HashMap<Integer, Boolean>();

        int LIMITE_RISERVE = 6; // per ora fisso, come in ControllerPrenotazioni

        for (Corso c : futuri) {
            int prenNormali = prenotazioneRepository.countByCorsoAndRiservaFalse(c);
            int prenTotali = prenotazioneRepository.countByCorso(c);

            prenotatiNormaliMap.put(c.getId(), prenNormali);
            prenotatiMap.put(c.getId(), prenTotali);

            boolean completoNormali = prenNormali >= c.getMaxPosti();
            boolean completoConRiserve = prenTotali >= c.getMaxPosti() + LIMITE_RISERVE;

            // può prenotare solo come riserva
            soloRiservaMap.put(c.getId(), completoNormali && !completoConRiserve);
            // corso davvero pieno (posti + riserve)
            pienoMap.put(c.getId(), completoConRiserve);
        }

        model.addAttribute("corsi", futuri);
        model.addAttribute("prenotatiMap", prenotatiMap);
        model.addAttribute("prenotatiNormaliMap", prenotatiNormaliMap);
        model.addAttribute("soloRiservaMap", soloRiservaMap);
        model.addAttribute("pienoMap", pienoMap);

        return "corsi";
    }

    // helper privato dentro ControllerCorsi
    private String giornoSettimanaItaliano(DayOfWeek dow) {
        return switch (dow) {
            case MONDAY -> "Lunedì";
            case TUESDAY -> "Martedì";
            case WEDNESDAY -> "Mercoledì";
            case THURSDAY -> "Giovedì";
            case FRIDAY -> "Venerdì";
            case SATURDAY -> "Sabato";
            case SUNDAY -> "Domenica";
        };
    }

    // GET /corsi/{id} -> dettaglio corso con posti disponibili
    @GetMapping("/corsi/{id}")
    public String dettaglioCorso(@PathVariable Integer id, Model model) {

        // 1) Carico il corso dall'id
        Corso corso = corsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Corso non trovato: " + id));

        // 2) Conteggio prenotazioni NORMALI (non riserva)
        int prenotatiNormali = prenotazioneRepository.countByCorsoAndRiservaFalse(corso);

        // (opzionale ma utile) Conteggio prenotazioni in RISERVA
        int prenotatiRiserve = prenotazioneRepository.countByCorsoAndRiservaTrue(corso);

        // 3) Calcolo posti disponibili (solo posti normali, non conto le riserve)
        int postiTotali = corso.getMaxPosti();
        int postiDisponibili = postiTotali - prenotatiNormali;
        if (postiDisponibili < 0) {
            postiDisponibili = 0;
        }

        // 4) Elenco prenotazioni (prima confermati, poi riserve; dentro: più vecchie
        // prima)
        List<Prenotazione> prenotazioniCorso = prenotazioneRepository.findByCorso(corso);

        Collections.sort(prenotazioniCorso, new java.util.Comparator<Prenotazione>() {
            @Override
            public int compare(Prenotazione p1, Prenotazione p2) {

                // Confermati prima delle riserve
                if (p1.isRiserva() && !p2.isRiserva())
                    return 1;
                if (!p1.isRiserva() && p2.isRiserva())
                    return -1;

                // Ordine per createdAt (più vecchi prima)
                if (p1.getCreatedAt() == null && p2.getCreatedAt() == null)
                    return 0;
                if (p1.getCreatedAt() == null)
                    return -1;
                if (p2.getCreatedAt() == null)
                    return 1;

                return p1.getCreatedAt().compareTo(p2.getCreatedAt());
            }
        });

        // Per badge “Nuovo” nelle ultime 24h
        LocalDateTime nowMinus24h = LocalDateTime.now().minusHours(24);

        // 5) Aggiungo attributi al model per Thymeleaf
        model.addAttribute("corso", corso);
        // qui "prenotati" ora significa SOLO prenotazioni normali
        model.addAttribute("prenotati", prenotatiNormali);
        model.addAttribute("postiDisponibili", postiDisponibili);
        model.addAttribute("prenotazioniCorso", prenotazioniCorso);
        model.addAttribute("nowMinus24h", nowMinus24h);

        // extra info su riserve (se vuoi usarle nel template dopo)
        model.addAttribute("prenotatiRiserve", prenotatiRiserve);
        model.addAttribute("maxRiserve", 6);

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

        // Calcolo capienza: posti normali + riserve
        int prenotatiNormali = prenotazioneRepository.countByCorsoAndRiservaFalse(corso);
        int prenotatiTotali = prenotazioneRepository.countByCorso(corso);
        int LIMITE_RISERVE = 6; // stesso valore di ControllerPrenotazioni

        int postiDisponibili = corso.getMaxPosti() - prenotatiNormali;
        if (postiDisponibili < 0) {
            postiDisponibili = 0;
        }

        int riserveDisponibili = corso.getMaxPosti() + LIMITE_RISERVE - prenotatiTotali;
        if (riserveDisponibili < 0) {
            riserveDisponibili = 0;
        }

        boolean soloRiserva = postiDisponibili == 0 && riserveDisponibili > 0;
        boolean completamentePieno = riserveDisponibili == 0;

        model.addAttribute("corso", corso);
        model.addAttribute("prenotatiNormali", prenotatiNormali);
        model.addAttribute("prenotatiTotali", prenotatiTotali);
        model.addAttribute("postiDisponibili", postiDisponibili);
        model.addAttribute("riserveDisponibili", riserveDisponibili);
        model.addAttribute("soloRiserva", soloRiserva);
        model.addAttribute("completamentePieno", completamentePieno);

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
