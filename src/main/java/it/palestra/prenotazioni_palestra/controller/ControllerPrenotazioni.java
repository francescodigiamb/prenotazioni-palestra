package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
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

    public ControllerPrenotazioni(CorsoRepository corsoRepository,
            UtenteRepository utenteRepository,
            PrenotazioneRepository prenotazioneRepository) {
        this.corsoRepository = corsoRepository;
        this.utenteRepository = utenteRepository;
        this.prenotazioneRepository = prenotazioneRepository;
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
        int prenotati = prenotazioneRepository.countByCorso(corso);
        if (prenotati >= corso.getMaxPosti()) {
            redirectAttrs.addFlashAttribute("error", "Corso al completo. Non è possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }

        // 7) Salva
        Prenotazione p = new Prenotazione(utente, corso);
        prenotazioneRepository.save(p);

        redirectAttrs.addFlashAttribute("success", "Prenotazione effettuata con successo!");
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
        model.addAttribute("email", email.trim());
        model.addAttribute("prenotazioni", prenotazioni);
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

        prenotazioneRepository.deleteById(prenotazioneId);
        redirectAttrs.addFlashAttribute("success", "Prenotazione cancellata correttamente.");
        return "redirect:/prenotazioni/mie";
    }
}
