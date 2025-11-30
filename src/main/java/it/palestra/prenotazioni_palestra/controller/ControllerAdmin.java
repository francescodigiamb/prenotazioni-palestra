package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.ModelloCorso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.ModelloCorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import it.palestra.prenotazioni_palestra.service.PianificazioneService;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Controller
@RequestMapping("/admin")
public class ControllerAdmin {

    private final CorsoRepository corsoRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final ModelloCorsoRepository modelloRepo;
    private final PianificazioneService pianificazioneService;

    // COSTRUTTORE
    public ControllerAdmin(CorsoRepository corsoRepository, PrenotazioneRepository prenotazioneRepository,
            ModelloCorsoRepository modelloRepo,
            PianificazioneService pianificazioneService) {
        this.corsoRepository = corsoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.modelloRepo = modelloRepo;
        this.pianificazioneService = pianificazioneService;
    }

    private boolean isExpired(Corso c) {
        LocalDate today = LocalDate.now();
        if (c.getData().isBefore(today))
            return true;
        return c.getData().isEqual(today) && c.getOrario().isBefore(LocalTime.now());
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/corsi"; // oppure ritorna "admin-dashboard"
    }

    @GetMapping("/corsi")
    public String corsiAdmin(Model model) {
        List<Corso> tutti = corsoRepository.findAll();
        // ordina per data/ora decrescente
        tutti.sort(java.util.Comparator
                .comparing(Corso::getData).reversed()
                .thenComparing(Corso::getOrario).reversed());

        Map<Integer, Integer> prenotatiMap = new java.util.HashMap<>();
        for (Corso c : tutti) {
            prenotatiMap.put(c.getId(), prenotazioneRepository.countByCorsoAndRiservaFalse(c));

        }

        model.addAttribute("corsi", tutti);
        model.addAttribute("prenotatiMap", prenotatiMap);
        return "admin-corsi"; // nuovo template
    }

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.web.bind.annotation.PostMapping("/corsi/{id}/toggle-chiusura")
    public String toggleChiusura(@org.springframework.web.bind.annotation.PathVariable Integer id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        var maybe = corsoRepository.findById(id);
        if (!maybe.isPresent()) {
            ra.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/admin/corsi";
        }
        Corso c = maybe.get();
        c.setChiuso(!c.isChiuso());
        corsoRepository.save(c);
        ra.addFlashAttribute("success", c.isChiuso()
                ? "Prenotazioni chiuse per il corso: " + c.getNome()
                : "Prenotazioni riaperte per il corso: " + c.getNome());
        return "redirect:/admin/corsi";
    }

    @GetMapping("/corsi/{id}")
    public String adminDettaglioCorso(@PathVariable Integer id, Model model) {

        // 1) Carico il corso
        Corso corso = corsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Corso non trovato: " + id));

        // 2) Divido le prenotazioni:
        // - confermati = riserva = false
        // - riserve = riserva = true
        List<Prenotazione> confermati = prenotazioneRepository.findByCorsoAndRiservaFalse(corso);
        List<Prenotazione> riserve = prenotazioneRepository.findByCorsoAndRiservaTrue(corso);

        // 3) Conteggi utili (se vuoi mostrarli)
        int prenotatiNormali = confermati.size();
        int prenotatiInAttesa = riserve.size();

        int postiTotali = corso.getMaxPosti();
        int postiDisponibili = postiTotali - prenotatiNormali;
        if (postiDisponibili < 0) {
            postiDisponibili = 0;
        }

        // 4) Aggiungo al model
        model.addAttribute("corso", corso);
        model.addAttribute("confermati", confermati);
        model.addAttribute("riserve", riserve);
        model.addAttribute("prenotatiNormali", prenotatiNormali);
        model.addAttribute("prenotatiInAttesa", prenotatiInAttesa);
        model.addAttribute("postiTotali", postiTotali);
        model.addAttribute("postiDisponibili", postiDisponibili);

        return "admin-corso-dettaglio"; // il template che mostra i dettagli per l'admin
    }

    // CORSI ARCHIVIATI
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
            prenotatiMap.put(c.getId(), prenotazioneRepository.countByCorsoAndRiservaFalse(c));

        }

        model.addAttribute("corsi", archiviati);
        model.addAttribute("prenotatiMap", prenotatiMap);
        return "admin-corsi-archiviati";
    }

    // Form nuovo corso
    @GetMapping("/corsi/nuovo")
    public String nuovoCorso(Model model) {
        model.addAttribute("corso", new Corso());
        return "admin-corso-form";
    }

    // Salva nuovo corso
    @PostMapping("/corsi")
    @org.springframework.transaction.annotation.Transactional
    public String creaCorso(@org.springframework.web.bind.annotation.ModelAttribute Corso corso,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {

        // validazione semplice: niente passato
        if (corso.getData() == null || corso.getOrario() == null) {
            ra.addFlashAttribute("error", "Data e orario sono obbligatori.");
            return "redirect:/admin/corsi/nuovo";
        }
        var today = java.time.LocalDate.now();
        var now = java.time.LocalTime.now();
        if (corso.getData().isBefore(today) ||
                (corso.getData().isEqual(today) && corso.getOrario().isBefore(now))) {
            ra.addFlashAttribute("warning", "Non puoi creare corsi nel passato.");
            return "redirect:/admin/corsi/nuovo";
        }
        if (corso.getMaxPosti() <= 0) {
            ra.addFlashAttribute("warning", "I posti devono essere maggiori di zero.");
            return "redirect:/admin/corsi/nuovo";
        }

        corsoRepository.save(corso);
        ra.addFlashAttribute("success", "Corso creato correttamente.");
        return "redirect:/admin/corsi";
    }

    // Form modifica
    @GetMapping("/corsi/{id}/modifica")
    public String modificaCorso(@org.springframework.web.bind.annotation.PathVariable Integer id, Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        var maybe = corsoRepository.findById(id);
        if (!maybe.isPresent()) {
            ra.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/admin/corsi";
        }
        model.addAttribute("corso", maybe.get());
        return "admin-corso-form";
    }

    // Update
    @PostMapping("/corsi/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String aggiornaCorso(@org.springframework.web.bind.annotation.PathVariable Integer id,
            @org.springframework.web.bind.annotation.ModelAttribute Corso form,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        var maybe = corsoRepository.findById(id);
        if (!maybe.isPresent()) {
            ra.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/admin/corsi";
        }
        Corso c = maybe.get();

        // blocco: non portare un corso nel passato
        var today = java.time.LocalDate.now();
        var now = java.time.LocalTime.now();
        if (form.getData() == null || form.getOrario() == null) {
            ra.addFlashAttribute("error", "Data e orario sono obbligatori.");
            return "redirect:/admin/corsi/" + id + "/modifica";
        }
        if (form.getData().isBefore(today) ||
                (form.getData().isEqual(today) && form.getOrario().isBefore(now))) {
            ra.addFlashAttribute("warning", "Non puoi impostare una data/ora nel passato.");
            return "redirect:/admin/corsi/" + id + "/modifica";
        }
        if (form.getMaxPosti() <= 0) {
            ra.addFlashAttribute("warning", "I posti devono essere maggiori di zero.");
            return "redirect:/admin/corsi/" + id + "/modifica";
        }

        // aggiorna campi
        c.setNome(form.getNome());
        c.setDescrizione(form.getDescrizione());
        c.setData(form.getData());
        c.setOrario(form.getOrario());
        c.setMaxPosti(form.getMaxPosti());
        c.setChiuso(form.isChiuso());

        corsoRepository.save(c);
        ra.addFlashAttribute("success", "Corso aggiornato correttamente.");
        return "redirect:/admin/corsi";
    }

    // Delete
    @PostMapping("/corsi/{id}/elimina")
    @org.springframework.transaction.annotation.Transactional
    public String eliminaCorso(@org.springframework.web.bind.annotation.PathVariable Integer id,
            org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        var maybe = corsoRepository.findById(id);
        if (!maybe.isPresent()) {
            ra.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/admin/corsi";
        }
        Corso c = maybe.get();

        int n = prenotazioneRepository.countByCorso(c); // quante prenotazioni collegate

        // 1) rimuovo prima le prenotazioni (per evitare violazioni FK)
        if (n > 0) {
            prenotazioneRepository.deleteByCorso(c);
        }

        // 2) poi elimino il corso
        corsoRepository.delete(c);

        ra.addFlashAttribute("success", n > 0
                ? ("Corso eliminato. Rimosse anche " + n + " prenotazioni collegate.")
                : "Corso eliminato.");
        return "redirect:/admin/corsi";
    }

    @GetMapping("/prenotazioni")
    public String adminPrenotazioni(@RequestParam(value = "corsoId", required = false) Integer corsoId,
            @RequestParam(value = "email", required = false) String email,
            Model model) {

        List<Prenotazione> prenotazioni;
        if (corsoId != null) {
            prenotazioni = prenotazioneRepository.findByCorso_Id(corsoId);
            model.addAttribute("filtro", "Corso ID: " + corsoId);
        } else if (email != null && !email.trim().isEmpty()) {
            prenotazioni = prenotazioneRepository.findByUtente_Email(email.trim());
            model.addAttribute("filtro", "Email: " + email.trim());
        } else {
            prenotazioni = prenotazioneRepository.findAll(); // semplice; se vuoi puoi ordinare
            model.addAttribute("filtro", "Tutte");
        }

        // opzionale: ordina per corso/data/ora
        prenotazioni.sort((a, b) -> {
            int cmp = a.getCorso().getData().compareTo(b.getCorso().getData());
            if (cmp != 0)
                return -cmp; // desc
            return -a.getCorso().getOrario().compareTo(b.getCorso().getOrario()); // desc
        });

        model.addAttribute("prenotazioni", prenotazioni);
        model.addAttribute("corsi", corsoRepository.findAll()); // per dropdown filtro
        return "admin-prenotazioni";
    }

    @Transactional
    @PostMapping("/prenotazioni/{id}/cancella")
    public String adminCancellaPrenotazione(@PathVariable Integer id, RedirectAttributes ra) {
        Optional<Prenotazione> maybe = prenotazioneRepository.findById(id);
        if (!maybe.isPresent()) {
            ra.addFlashAttribute("error", "Prenotazione non trovata.");
            return "redirect:/admin/prenotazioni";
        }
        prenotazioneRepository.deleteById(id);
        ra.addFlashAttribute("success", "Prenotazione cancellata.");
        return "redirect:/admin/prenotazioni";
    }

    // MODELLI CORSI
    // LISTA modelli
    @GetMapping("/modelli")
    public String listaModelli(Model model) {
        model.addAttribute("modelli", modelloRepo.findAll());
        return "admin-modelli";
    }

    // NUOVO modello
    @GetMapping("/modelli/nuovo")
    public String nuovoModello(Model model) {
        model.addAttribute("modello", new ModelloCorso());
        return "admin-modello-form";
    }

    // MODIFICA modello esistente
    @GetMapping("/modelli/{id}/modifica")
    public String modificaModello(@PathVariable Integer id, Model model, RedirectAttributes ra) {
        ModelloCorso m = modelloRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Modello non trovato: " + id));
        model.addAttribute("modello", m);
        return "admin-modello-form";
    }

    // CREA/AGGIORNA modello (UNICO POST)
    @PostMapping("/modelli")
    public String salvaModello(@ModelAttribute("modello") ModelloCorso modello,
            RedirectAttributes ra) {

        // se modello.id è null → insert, se non è null → update
        modelloRepo.save(modello);
        ra.addFlashAttribute("success", "Modello corso salvato.");
        return "redirect:/admin/modelli";
    }

    // GENERA OCCORRENZE (CORSI) A PARTIRE DA UN MODELLO
    @PostMapping("/modelli/{id}/genera")
    public String generaCorsiDaModello(
            @PathVariable("id") Integer id,
            @RequestParam("dal") String dalStr,
            @RequestParam("al") String alStr,
            RedirectAttributes ra) {

        LocalDate dal = LocalDate.parse(dalStr);
        LocalDate al = LocalDate.parse(alStr);

        if (al.isBefore(dal)) {
            ra.addFlashAttribute("error", "La data di fine deve essere successiva o uguale alla data di inizio.");
            return "redirect:/admin/modelli";
        }

        int creati = pianificazioneService.generaOccorrenze(id, dal, al);

        if (creati > 0) {
            ra.addFlashAttribute("success",
                    "Generati " + creati + " corsi dal " + dal + " al " + al + ".");
        } else {
            ra.addFlashAttribute("info",
                    "Nessun corso generato: controlla i giorni/orari abilitati nel modello.");
        }

        return "redirect:/admin/modelli";
    }

    // PROMUOVI RISERVA
    @PostMapping("/admin/corsi/{id}/promuovi")
    public String promuoviRiserva(@PathVariable Integer id,
            @RequestParam("prenotazioneId") Integer prenotazioneId,
            RedirectAttributes ra) {

        Corso corso = corsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Corso non trovato"));

        int prenNormali = prenotazioneRepository.countByCorsoAndRiservaFalse(corso);
        if (prenNormali >= corso.getMaxPosti()) {
            ra.addFlashAttribute("error", "Nessun posto normale disponibile.");
            return "redirect:/admin/corsi/" + id;
        }

        Prenotazione p = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new IllegalArgumentException("Prenotazione non trovata"));

        p.setRiserva(false);
        prenotazioneRepository.save(p);

        ra.addFlashAttribute("success", "Prenotazione promossa dalla lista d'attesa.");
        return "redirect:/admin/corsi/" + id;
    }

}
