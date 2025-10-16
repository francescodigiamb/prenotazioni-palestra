package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Controller
@RequestMapping("/prenotazioni")
public class ControllerPrenotazioni {

    @Autowired
    private CorsoRepository corsoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    private static final Pattern EMAIL_RE = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * Crea una prenotazione.
     */
    @Transactional
    @PostMapping
    public String creaPrenotazione(@RequestParam("corsoId") Integer corsoId,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "nome", required = false) String nome,
            RedirectAttributes redirectAttrs) {
        // Validazioni base lato controller (prima di toccare il DB)
        if (nome == null || nome.isBlank()) {
            redirectAttrs.addFlashAttribute("warning", "Inserisci il nome.");
            return "redirect:/corsi/" + corsoId + "/prenota";
        }
        if (email == null || !EMAIL_RE.matcher(email).matches()) {
            redirectAttrs.addFlashAttribute("warning", "Inserisci un'email valida.");
            return "redirect:/corsi/" + corsoId + "/prenota";
        }

        // 1) Corso con LOCK (anti-race)
        Optional<Corso> maybeCorso = corsoRepository.lockById(corsoId);
        if (!maybeCorso.isPresent()) {
            redirectAttrs.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/corsi";
        }
        Corso corso = maybeCorso.get();

        if (corso.getData().isBefore(LocalDate.now()) ||
                (corso.getData().isEqual(LocalDate.now()) && corso.getOrario().isBefore(LocalTime.now()))) {
            redirectAttrs.addFlashAttribute("error", "Il corso è scaduto: non è più possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }

        // 2) Utente (recupera o crea)
        Utente utente;
        Optional<Utente> maybeUtente = utenteRepository.findByEmail(email);
        if (maybeUtente.isPresent()) {
            utente = maybeUtente.get();
            if (nome != null && !nome.trim().isEmpty()
                    && (utente.getNome() == null || !utente.getNome().equals(nome))) {
                utente.setNome(nome);
                utenteRepository.save(utente);
            }
        } else {
            utente = new Utente();
            utente.setNome(nome);
            utente.setEmail(email);
            utente.setRuolo("UTENTE");
            utente.setPassword("123456"); // provvisoria; da gestire quando faremo la security
            utente = utenteRepository.save(utente);
        }

        // 3) Check duplicato e capienza *sotto lock*
        if (prenotazioneRepository.existsByUtenteAndCorso(utente, corso)) {
            redirectAttrs.addFlashAttribute("warning", "Sei già prenotato a questo corso.");
            return "redirect:/corsi/" + corsoId;
        }

        int prenotati = prenotazioneRepository.countByCorso(corso);
        if (prenotati >= corso.getMaxPosti()) {
            redirectAttrs.addFlashAttribute("error", "Corso al completo. Non è possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }

        // 4) Salva con rete di sicurezza sul vincolo unico
        try {
            Prenotazione p = new Prenotazione(utente, corso);
            prenotazioneRepository.save(p);

        } catch (DataIntegrityViolationException e) {
            // Scatta se due richieste simultanee tentano di inserire la stessa prenotazione
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

        if (email == null || email.trim().isEmpty()) {
            model.addAttribute("email", "");
            model.addAttribute("prenotazioni", Collections.emptyList());
            return "prenotazioni-mie";
        }

        List<Prenotazione> prenotazioni = prenotazioneRepository.findByUtente_Email(email);
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
