package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import it.palestra.prenotazioni_palestra.service.EmailService;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/prenotazioni")
public class ControllerPrenotazioni {

    private final CorsoRepository corsoRepository;
    private final UtenteRepository utenteRepository;
    private final PrenotazioneRepository prenotazioneRepository;
    private final EmailService emailService;

    public ControllerPrenotazioni(CorsoRepository corsoRepository,
            UtenteRepository utenteRepository,
            PrenotazioneRepository prenotazioneRepository, EmailService emailService) {
        this.corsoRepository = corsoRepository;
        this.utenteRepository = utenteRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.emailService = emailService;
    }

    // ====== CREA PRENOTAZIONE (POST) ======
    @Transactional
    @PostMapping
    public String creaPrenotazione(@RequestParam("corsoId") Integer corsoId,
            @RequestParam(value = "nome", required = false) String nome,
            RedirectAttributes redirectAttrs) {

        // 1) Utente autenticato
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : null;
        if (email == null || "anonymousUser".equalsIgnoreCase(email)) {
            redirectAttrs.addFlashAttribute("error", "Devi accedere per prenotare.");
            return "redirect:/login";
        }

        // 2) Utente deve esistere (niente creazione al volo)
        Optional<Utente> maybeUtente = utenteRepository.findByEmail(email.trim());
        if (!maybeUtente.isPresent()) {
            redirectAttrs.addFlashAttribute("error", "Account non valido. Effettua nuovamente l'accesso.");
            return "redirect:/login";
        }
        Utente utente = maybeUtente.get();

        // (facoltativo) aggiorna nome profilo se fornito e diverso
        if (nome != null && !nome.trim().isEmpty()
                && (utente.getNome() == null || !utente.getNome().equals(nome.trim()))) {
            utente.setNome(nome.trim());
            utenteRepository.save(utente);
        }

        // 3) Corso (se hai un metodo lockById usa quello; qui usiamo findById per
        // compatibilità)
        Optional<Corso> maybeCorso = corsoRepository.findById(corsoId);
        if (!maybeCorso.isPresent()) {
            redirectAttrs.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/corsi";
        }
        Corso corso = maybeCorso.get();

        // 4) Blocchi: scaduto / chiuso
        if (corso.getData().isBefore(LocalDate.now()) ||
                (corso.getData().isEqual(LocalDate.now()) && corso.getOrario().isBefore(LocalTime.now()))) {
            redirectAttrs.addFlashAttribute("error", "Il corso è scaduto: non è più possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }
        if (corso.isChiuso()) {
            redirectAttrs.addFlashAttribute("warning", "Le prenotazioni per questo corso sono momentaneamente chiuse.");
            return "redirect:/corsi/" + corsoId;
        }

        // 5) Doppia prenotazione
        if (prenotazioneRepository.existsByUtenteAndCorso(utente, corso)) {
            redirectAttrs.addFlashAttribute("warning", "Sei già prenotato a questo corso.");
            return "redirect:/corsi/" + corsoId;
        }

        // 6) Capienza
        int prenotatiNormali = prenotazioneRepository.countByCorsoAndRiservaFalse(corso);
        int prenotatiTotali = prenotazioneRepository.countByCorso(corso);
        int LIMITE_RISERVE = 6;

        // 1) Se ci sono ancora posti normali
        if (prenotatiNormali < corso.getMaxPosti()) {
            // prenotazione normale
            Prenotazione p = new Prenotazione(utente, corso);
            p.setRiserva(false);
            prenotazioneRepository.save(p);

            redirectAttrs.addFlashAttribute("success", "Prenotazione confermata!");
            return "redirect:/corsi/" + corsoId;
        }

        // 2) Se posti normali finiscono ma riserve NON sono piene

        if (prenotatiTotali < corso.getMaxPosti() + LIMITE_RISERVE) {
            // prenotazione in riserva
            Prenotazione p = new Prenotazione(utente, corso);
            p.setRiserva(true);
            prenotazioneRepository.save(p);

            redirectAttrs.addFlashAttribute("warning",
                    "Il corso è pieno: sei stato inserito in lista d'attesa (In attesa).");
            return "redirect:/corsi/" + corsoId;
        }

        // 3) Se anche le riserve sono piene → KO
        redirectAttrs.addFlashAttribute("error", "Il corso è al completo.");
        return "redirect:/corsi/" + corsoId;

    }

    // ====== LE MIE PRENOTAZIONI (GET) ======
    @GetMapping("/mie")
    public String miePrenotazioni(@RequestParam(value = "email", required = false) String email,
            Model model) {

        // se non arriva dal parametro, usa l'utente loggato
        if (email == null || email.trim().isEmpty()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                email = auth.getName();
            }
        }

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("email", "");
            model.addAttribute("prenotazioni", Collections.emptyList());
            return "prenotazioni-mie";
        }

        List<Prenotazione> prenotazioni = prenotazioneRepository.findByUtente_Email(email.trim());

        // Badge di validità
        java.util.Map<Integer, String> statoMap = new java.util.HashMap<Integer, String>(); // p.id ->
                                                                                            // "Valida"/"Oggi"/"Scaduta"
        java.util.Map<Integer, String> coloreMap = new java.util.HashMap<Integer, String>(); // p.id ->
                                                                                             // "success"/"warning"/"secondary"

        // Liste separate
        List<Prenotazione> prenotazioniAttive = new java.util.ArrayList<Prenotazione>();
        List<Prenotazione> prenotazioniArchivio = new java.util.ArrayList<Prenotazione>();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalTime now = java.time.LocalTime.now();

        for (Prenotazione p : prenotazioni) {
            if (p == null || p.getId() == null) {
                continue;
            }

            Corso corso = p.getCorso();
            if (corso == null || corso.getData() == null || corso.getOrario() == null) {
                // se manca qualcosa, la consideriamo "attiva" per non buttarla in archivio per
                // errore
                prenotazioniAttive.add(p);
                statoMap.put(p.getId(), "Valida");
                coloreMap.put(p.getId(), "success");
                continue;
            }

            java.time.LocalDate d = corso.getData();
            java.time.LocalTime t = corso.getOrario();

            boolean expired = d.isBefore(today) || (d.isEqual(today) && !t.isAfter(now));

            if (expired) {
                prenotazioniArchivio.add(p);
                statoMap.put(p.getId(), "Scaduta");
                coloreMap.put(p.getId(), "secondary");
            } else if (d.isEqual(today)) {
                prenotazioniAttive.add(p);
                statoMap.put(p.getId(), "Oggi");
                coloreMap.put(p.getId(), "warning");
            } else {
                prenotazioniAttive.add(p);
                statoMap.put(p.getId(), "Valida");
                coloreMap.put(p.getId(), "success");
            }
        }

        // Ordinamento attive: data ASC, ora ASC
        java.util.Collections.sort(prenotazioniAttive, new java.util.Comparator<Prenotazione>() {
            @Override
            public int compare(Prenotazione a, Prenotazione b) {
                Corso ca = (a != null) ? a.getCorso() : null;
                Corso cb = (b != null) ? b.getCorso() : null;

                if (ca == null || cb == null || ca.getData() == null || cb.getData() == null) {
                    return 0;
                }

                int cmpData = ca.getData().compareTo(cb.getData());
                if (cmpData != 0)
                    return cmpData;

                if (ca.getOrario() == null || cb.getOrario() == null) {
                    return 0;
                }

                int cmpOra = ca.getOrario().compareTo(cb.getOrario());
                if (cmpOra != 0)
                    return cmpOra;

                return 0;
            }
        });

        // Ordinamento archivio: data DESC, ora DESC
        java.util.Collections.sort(prenotazioniArchivio, new java.util.Comparator<Prenotazione>() {
            @Override
            public int compare(Prenotazione a, Prenotazione b) {
                Corso ca = (a != null) ? a.getCorso() : null;
                Corso cb = (b != null) ? b.getCorso() : null;

                if (ca == null || cb == null || ca.getData() == null || cb.getData() == null) {
                    return 0;
                }

                int cmpData = cb.getData().compareTo(ca.getData());
                if (cmpData != 0)
                    return cmpData;

                if (ca.getOrario() == null || cb.getOrario() == null) {
                    return 0;
                }

                int cmpOra = cb.getOrario().compareTo(ca.getOrario());
                if (cmpOra != 0)
                    return cmpOra;

                return 0;
            }
        });

        model.addAttribute("email", email);
        model.addAttribute("prenotazioniAttive", prenotazioniAttive);
        model.addAttribute("prenotazioniArchivio", prenotazioniArchivio);

        // se vuoi mantenere compatibilità con template vecchio:
        model.addAttribute("prenotazioni", prenotazioniAttive);

        model.addAttribute("statoMap", statoMap);
        model.addAttribute("coloreMap", coloreMap);

        return "prenotazioni-mie";

    }

    // ====== CANCELLA PRENOTAZIONE (POST) ======
    @PostMapping("/{id}/cancella")
    public String cancellaPrenotazione(@PathVariable("id") Integer prenotazioneId,
            RedirectAttributes redirectAttrs) {

        // email dall'utente loggato
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : null;
        if (email == null || "anonymousUser".equalsIgnoreCase(email)) {
            redirectAttrs.addFlashAttribute("error", "Devi accedere per gestire le tue prenotazioni.");
            return "redirect:/login";
        }

        Optional<Prenotazione> maybeP = prenotazioneRepository.findById(prenotazioneId);
        if (!maybeP.isPresent()) {
            redirectAttrs.addFlashAttribute("error", "Prenotazione non trovata.");
            return "redirect:/prenotazioni/mie";
        }

        Prenotazione p = maybeP.get();

        // Solo il proprietario può cancellare
        if (p.getUtente() == null || p.getUtente().getEmail() == null
                || !p.getUtente().getEmail().equalsIgnoreCase(email)) {
            redirectAttrs.addFlashAttribute("error", "Non sei autorizzato a cancellare questa prenotazione.");
            return "redirect:/prenotazioni/mie";
        }

        // ✅ Salvo i dati PRIMA della delete (servono per la mail admin)
        String corsoNome = (p.getCorso() != null && p.getCorso().getNome() != null) ? p.getCorso().getNome() : "-";
        String data = (p.getCorso() != null && p.getCorso().getData() != null)
                ? p.getCorso().getData().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                : "-";
        String ora = (p.getCorso() != null && p.getCorso().getOrario() != null)
                ? p.getCorso().getOrario().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                : "-";

        String utenteNome = (p.getUtente() != null && p.getUtente().getNome() != null) ? p.getUtente().getNome() : "-";
        String utenteCognome = (p.getUtente() != null && p.getUtente().getCognome() != null)
                ? p.getUtente().getCognome()
                : "-";
        String utenteEmail = (p.getUtente() != null && p.getUtente().getEmail() != null) ? p.getUtente().getEmail()
                : "-";

        // delete
        prenotazioneRepository.deleteById(prenotazioneId);

        // ✅ MAIL admin: disdetta
        emailService.inviaNotificaDisdettaAdmin(corsoNome, data, ora, utenteNome, utenteCognome, utenteEmail);

        redirectAttrs.addFlashAttribute("success", "Prenotazione cancellata correttamente.");
        return "redirect:/prenotazioni/mie";
    }

}
