package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.ModelloCorso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.ModelloCorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import it.palestra.prenotazioni_palestra.service.CleanupService;
import it.palestra.prenotazioni_palestra.service.PianificazioneService;

import org.springframework.format.annotation.DateTimeFormat;
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
    private final CleanupService cleanupService;

    // COSTRUTTORE
    public ControllerAdmin(CorsoRepository corsoRepository, PrenotazioneRepository prenotazioneRepository,
            ModelloCorsoRepository modelloRepo,
            PianificazioneService pianificazioneService, CleanupService cleanupService) {
        this.corsoRepository = corsoRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.modelloRepo = modelloRepo;
        this.pianificazioneService = pianificazioneService;
        this.cleanupService = cleanupService;
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
    public String corsiAdmin(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String q,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dal,
            @org.springframework.web.bind.annotation.RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate al,
            @org.springframework.web.bind.annotation.RequestParam(required = false, defaultValue = "tutti") String stato,
            Model model) {

        List<Corso> tutti = corsoRepository.findAll();

        // SOLO corsi non scaduti (quelli scaduti stanno in Archiviati)
        List<Corso> visibili = tutti.stream()
                .filter(c -> !isExpired(c))
                .toList();

        // filtri
        String qNorm = (q == null) ? "" : q.trim().toLowerCase();

        List<Corso> filtrati = visibili.stream()
                .filter(c -> qNorm.isBlank() || (c.getNome() != null && c.getNome().toLowerCase().contains(qNorm)))
                .filter(c -> dal == null || !c.getData().isBefore(dal))
                .filter(c -> al == null || !c.getData().isAfter(al))
                .filter(c -> "tutti".equalsIgnoreCase(stato)
                        || ("aperti".equalsIgnoreCase(stato) && !c.isChiuso())
                        || ("chiusi".equalsIgnoreCase(stato) && c.isChiuso()))
                // ordinamento: prossimi corsi prima (più comodo in gestione)
                .sorted(java.util.Comparator
                        .comparing(Corso::getData)
                        .thenComparing(Corso::getOrario))
                .toList();

        Map<Integer, Integer> prenotatiMap = new java.util.HashMap<>();
        for (Corso c : filtrati) {
            prenotatiMap.put(c.getId(), prenotazioneRepository.countByCorsoAndRiservaFalse(c));
        }

        // per precompilare il form filtri
        model.addAttribute("q", q);
        model.addAttribute("dal", dal);
        model.addAttribute("al", al);
        model.addAttribute("stato", stato);

        model.addAttribute("corsi", filtrati);
        model.addAttribute("prenotatiMap", prenotatiMap);

        return "admin-corsi";
    }

    @GetMapping("/corsi/{id}")
    public String adminDettaglioCorso(@PathVariable Integer id, Model model) {

        Corso corso = corsoRepository.findById(id).orElse(null);
        if (corso == null) {
            model.addAttribute("error", "Corso non trovato.");
            return "redirect:/admin/prenotazioni";
        }

        List<Prenotazione> prenotazioni = prenotazioneRepository.findByCorso_Id(id);

        // Ordinamento: confermati prima, poi riserve; dentro: createdAt più vecchi
        // prima
        Collections.sort(prenotazioni, new Comparator<Prenotazione>() {
            @Override
            public int compare(Prenotazione p1, Prenotazione p2) {

                // confermati prima delle riserve
                if (p1.isRiserva() && !p2.isRiserva())
                    return 1;
                if (!p1.isRiserva() && p2.isRiserva())
                    return -1;

                // createdAt (più vecchi prima)
                if (p1.getCreatedAt() == null && p2.getCreatedAt() == null)
                    return 0;
                if (p1.getCreatedAt() == null)
                    return -1;
                if (p2.getCreatedAt() == null)
                    return 1;

                return p1.getCreatedAt().compareTo(p2.getCreatedAt());
            }
        });

        int confermati = 0;
        int riserve = 0;

        for (Prenotazione p : prenotazioni) {
            if (p == null)
                continue;
            if (p.isRiserva()) {
                riserve++;
            } else {
                confermati++;
            }
        }

        Integer maxPosti = corso.getMaxPosti();
        int postiDisponibili = 0;
        if (maxPosti != null) {
            postiDisponibili = maxPosti.intValue() - confermati;
            if (postiDisponibili < 0)
                postiDisponibili = 0;
        }

        model.addAttribute("corso", corso);
        model.addAttribute("prenotazioni", prenotazioni);
        model.addAttribute("confermati", confermati);
        model.addAttribute("riserve", riserve);
        model.addAttribute("postiDisponibili", postiDisponibili);

        return "admin-corso-dettaglio";
    }

    // CORSI ARCHIVIATI
    @GetMapping("/corsi-archiviati")
    public String corsiArchiviati(Model model) {

        List<Corso> tutti = corsoRepository.findAll();

        List<Corso> archiviati = new ArrayList<Corso>();
        for (Corso c : tutti) {
            if (c == null)
                continue;
            if (isExpired(c)) {
                archiviati.add(c);
            }
        }

        // ordine: DESC (ultimi svolti in alto)
        Collections.sort(archiviati, new Comparator<Corso>() {
            @Override
            public int compare(Corso c1, Corso c2) {
                int cmpData = c2.getData().compareTo(c1.getData());
                if (cmpData != 0)
                    return cmpData;
                return c2.getOrario().compareTo(c1.getOrario());
            }
        });

        java.util.Map<Integer, Integer> prenotatiMap = new java.util.HashMap<Integer, Integer>();
        java.util.Map<Integer, Integer> riserveMap = new java.util.HashMap<Integer, Integer>();

        for (Corso c : archiviati) {
            prenotatiMap.put(c.getId(), prenotazioneRepository.countByCorsoAndRiservaFalse(c));
            riserveMap.put(c.getId(), prenotazioneRepository.countByCorsoAndRiservaTrue(c));
        }

        model.addAttribute("corsi", archiviati);
        model.addAttribute("prenotatiMap", prenotatiMap);
        model.addAttribute("riserveMap", riserveMap);

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
    public String adminPrenotazioni(
            @RequestParam(value = "corsoId", required = false) Integer corsoId,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "dataDa", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataDa,
            @RequestParam(value = "dataA", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataA,
            Model model) {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        // 1) Parto da tutti i corsi
        List<Corso> tuttiCorsi = corsoRepository.findAll();

        // 2) Tengo SOLO corsi attivi (oggi/futuri)
        List<Corso> corsiAttivi = new ArrayList<Corso>();
        for (Corso c : tuttiCorsi) {
            if (c == null || c.getData() == null || c.getOrario() == null) {
                continue;
            }
            boolean expired = c.getData().isBefore(today)
                    || (c.getData().isEqual(today) && !c.getOrario().isAfter(now));
            if (!expired) {
                corsiAttivi.add(c);
            }
        }

        // 3) Applico filtri: corsoId, dataDa, dataA
        if (corsoId != null) {
            List<Corso> filtrati = new ArrayList<Corso>();
            for (Corso c : corsiAttivi) {
                if (c.getId() != null && c.getId().equals(corsoId)) {
                    filtrati.add(c);
                }
            }
            corsiAttivi = filtrati;
        }

        if (dataDa != null) {
            List<Corso> filtrati = new ArrayList<Corso>();
            for (Corso c : corsiAttivi) {
                if (c.getData() != null && !c.getData().isBefore(dataDa)) {
                    filtrati.add(c);
                }
            }
            corsiAttivi = filtrati;
        }

        if (dataA != null) {
            List<Corso> filtrati = new ArrayList<Corso>();
            for (Corso c : corsiAttivi) {
                if (c.getData() != null && !c.getData().isAfter(dataA)) {
                    filtrati.add(c);
                }
            }
            corsiAttivi = filtrati;
        }

        // 4) Filtro email: tengo solo i corsi dove quell'utente è prenotato (se
        // valorizzata)
        String emailTrim = null;
        if (email != null && !email.trim().isEmpty()) {
            emailTrim = email.trim();

            List<Prenotazione> prenUtente = prenotazioneRepository.findByUtente_Email(emailTrim);
            java.util.Set<Integer> corsoIds = new java.util.HashSet<Integer>();
            for (Prenotazione p : prenUtente) {
                if (p != null && p.getCorso() != null && p.getCorso().getId() != null) {
                    corsoIds.add(p.getCorso().getId());
                }
            }

            List<Corso> filtrati = new ArrayList<Corso>();
            for (Corso c : corsiAttivi) {
                if (c.getId() != null && corsoIds.contains(c.getId())) {
                    filtrati.add(c);
                }
            }
            corsiAttivi = filtrati;
        }

        // 5) Ordine cronologico (data ASC, ora ASC)
        Collections.sort(corsiAttivi, new Comparator<Corso>() {
            @Override
            public int compare(Corso c1, Corso c2) {
                int cmpData = c1.getData().compareTo(c2.getData());
                if (cmpData != 0)
                    return cmpData;
                return c1.getOrario().compareTo(c2.getOrario());
            }
        });

        // 6) Conteggi prenotati/riserve per corso
        java.util.Map<Integer, Integer> confermatiMap = new java.util.HashMap<Integer, Integer>();
        java.util.Map<Integer, Integer> riserveMap = new java.util.HashMap<Integer, Integer>();

        for (Corso c : corsiAttivi) {
            confermatiMap.put(c.getId(), prenotazioneRepository.countByCorsoAndRiservaFalse(c));
            riserveMap.put(c.getId(), prenotazioneRepository.countByCorsoAndRiservaTrue(c));
        }

        // 7) Model
        model.addAttribute("corsi", corsiAttivi);
        model.addAttribute("confermatiMap", confermatiMap);
        model.addAttribute("riserveMap", riserveMap);

        // sticky filtri
        model.addAttribute("selectedCorsoId", corsoId);
        model.addAttribute("emailFilter", emailTrim != null ? emailTrim : email);
        model.addAttribute("dataDa", dataDa);
        model.addAttribute("dataA", dataA);

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
    @PostMapping("/corsi/{id}/promuovi")
    public String promuoviRiserva(@PathVariable Integer id,
            @RequestParam("prenotazioneId") Integer prenotazioneId,
            RedirectAttributes ra) {

        Corso corso = corsoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Corso non trovato"));

        int prenNormali = prenotazioneRepository.countByCorsoAndRiservaFalse(corso);
        if (prenNormali >= corso.getMaxPosti()) {
            ra.addFlashAttribute("error", "Nessun posto normale disponibile.");
            return "redirect:/admin/prenotazioni?corsoId=" + id;
        }

        Prenotazione p = prenotazioneRepository.findById(prenotazioneId)
                .orElseThrow(() -> new IllegalArgumentException("Prenotazione non trovata"));

        p.setRiserva(false);
        prenotazioneRepository.save(p);

        ra.addFlashAttribute("success", "Prenotazione promossa dalla lista d'attesa.");
        return "redirect:/admin/prenotazioni?corsoId=" + id;
    }

    // Eliminazione corsi
    @PostMapping("/corsi-archiviati/elimina-tutti")
    public String eliminaTuttiCorsiArchiviati(RedirectAttributes redirectAttributes) {

        int eliminati = cleanupService.eliminaTuttiCorsiArchiviati();

        redirectAttributes.addFlashAttribute("success", "Eliminati " + eliminati + " corsi archiviati.");
        return "redirect:/admin/corsi-archiviati";
    }

}
