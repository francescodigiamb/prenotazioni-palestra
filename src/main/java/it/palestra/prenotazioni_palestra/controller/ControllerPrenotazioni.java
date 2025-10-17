package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Controller
@RequestMapping("/prenotazioni")
public class ControllerPrenotazioni {

    @Autowired
    private CorsoRepository corsoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    // inietta il PasswordEncoder se devi ancora creare utenti al volo:
    private final PasswordEncoder passwordEncoder;

    public ControllerPrenotazioni(CorsoRepository corsoRepository,
            UtenteRepository utenteRepository,
            PrenotazioneRepository prenotazioneRepository,
            PasswordEncoder passwordEncoder) {
        this.corsoRepository = corsoRepository;
        this.utenteRepository = utenteRepository;
        this.prenotazioneRepository = prenotazioneRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // Crea una prenotazione.

    @Transactional
    @PostMapping
    public String creaPrenotazione(@RequestParam("corsoId") Integer corsoId,
            @RequestParam(value = "nome", required = false) String nome,
            RedirectAttributes redirectAttrs) {

        // 1) Utente autenticato
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = (auth != null) ? auth.getName() : null;
        if (email == null) {
            redirectAttrs.addFlashAttribute("error", "Devi accedere per prenotare.");
            return "redirect:/login";
        }

        // 2) Validazioni base nome (facoltativo, puoi anche usare quello già salvato su
        // utente)
        if (nome == null || nome.isBlank()) {
            redirectAttrs.addFlashAttribute("warning", "Inserisci il nome.");
            return "redirect:/corsi/" + corsoId + "/prenota";
        }

        // 3) Corso con LOCK (anti-overbooking)
        Optional<Corso> maybeCorso = corsoRepository.lockById(corsoId);
        if (!maybeCorso.isPresent()) {
            redirectAttrs.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/corsi";
        }
        Corso corso = maybeCorso.get();

        // 4) Blocca corsi scaduti
        if (corso.getData().isBefore(LocalDate.now()) ||
                (corso.getData().isEqual(LocalDate.now()) && corso.getOrario().isBefore(LocalTime.now()))) {
            redirectAttrs.addFlashAttribute("error", "Il corso è scaduto: non è più possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }
        // blocca se corsi chiusi
        if (corso.isChiuso()) {
            redirectAttrs.addFlashAttribute("warning", "Le prenotazioni per questo corso sono momentaneamente chiuse.");
            return "redirect:/corsi/" + corsoId;
        }

        // 5) Utente (recupero/aggiornamento)
        Utente utente = utenteRepository.findByEmail(email).orElse(null);
        if (utente == null) {
            utente = new Utente();
            utente.setEmail(email);
            utente.setNome(nome);
            utente.setRuolo("UTENTE");
            utente.setPassword(passwordEncoder.encode("123456")); // placeholder finché non c'è registrazione
            utente = utenteRepository.save(utente);
        } else if (utente.getNome() == null || !utente.getNome().equals(nome)) {
            utente.setNome(nome);
            utenteRepository.save(utente);
        }

        // 6) Doppione + capienza (sotto lock)
        if (prenotazioneRepository.existsByUtenteAndCorso(utente, corso)) {
            redirectAttrs.addFlashAttribute("warning", "Sei già prenotato a questo corso.");
            return "redirect:/corsi/" + corsoId;
        }
        int prenotati = prenotazioneRepository.countByCorso(corso);
        if (prenotati >= corso.getMaxPosti()) {
            redirectAttrs.addFlashAttribute("error", "Corso al completo. Non è possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }

        // 7) Salva con rete di sicurezza
        try {
            Prenotazione p = new Prenotazione(utente, corso);
            prenotazioneRepository.save(p);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            redirectAttrs.addFlashAttribute("warning", "Sei già prenotato a questo corso.");
            return "redirect:/corsi/" + corsoId;
        }

        redirectAttrs.addFlashAttribute("success", "Prenotazione effettuata con successo!");
        return "redirect:/corsi/" + corsoId;
    }

    /**
     * Elenco delle prenotazioni dell'utente (per email).
     * Se l'email non è passata, mostra la pagina con form vuoto.
     */
    @GetMapping("/mie")
    public String miePrenotazioni(@RequestParam(value = "email", required = false) String email,
            Model model) {

        // Se non arriva dal param, prendi l'email dal SecurityContext
        if (email == null || email.trim().isEmpty()) {
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                email = auth.getName(); // username = email
            }
        }

        if (email == null || email.trim().isEmpty()) {
            // fallback: nessuna email disponibile -> lista vuota ma pagina carica
            model.addAttribute("email", "");
            model.addAttribute("prenotazioni", java.util.Collections.emptyList());
            return "prenotazioni-mie";
        }

        // carica prenotazioni dell'utente loggato
        java.util.List<it.palestra.prenotazioni_palestra.model.Prenotazione> prenotazioni = prenotazioneRepository
                .findByUtente_Email(email);

        model.addAttribute("email", email);
        model.addAttribute("prenotazioni", prenotazioni);
        return "prenotazioni-mie";
    }

    /**
     * Cancella una prenotazione (controllo base sull'email del proprietario).
     */
    @PostMapping("/{id}/cancella")
    public String cancellaPrenotazione(@PathVariable("id") Integer prenotazioneId,
            @RequestParam("email") String email,
            RedirectAttributes redirectAttrs) {

        Optional<Prenotazione> maybeP = prenotazioneRepository.findById(prenotazioneId);
        if (!maybeP.isPresent()) {
            redirectAttrs.addFlashAttribute("error", "Prenotazione non trovata.");
            return "redirect:/prenotazioni/mie?email=" + email;
        }

        Prenotazione p = maybeP.get();

        // Sicurezza minimale: solo il proprietario (stessa email) può disdire
        if (p.getUtente() == null || p.getUtente().getEmail() == null
                || !p.getUtente().getEmail().equalsIgnoreCase(email)) {
            redirectAttrs.addFlashAttribute("error", "Non sei autorizzato a cancellare questa prenotazione.");
            return "redirect:/prenotazioni/mie?email=" + email;
        }

        prenotazioneRepository.deleteById(prenotazioneId);
        redirectAttrs.addFlashAttribute("success", "Prenotazione cancellata correttamente.");
        return "redirect:/prenotazioni/mie?email=" + email;
    }
}
