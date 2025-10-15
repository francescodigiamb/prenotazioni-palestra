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

import java.util.Collections;
import java.util.List;
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

    /**
     * Crea una prenotazione.
     */
    @Transactional
    @PostMapping
    public String creaPrenotazione(@RequestParam("corsoId") Integer corsoId,
            @RequestParam("email") String email,
            @RequestParam("nome") String nome,
            RedirectAttributes redirectAttrs) {

        // 1) Corso
        Optional<Corso> maybeCorso = corsoRepository.findById(corsoId);
        if (!maybeCorso.isPresent()) {
            redirectAttrs.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/corsi";
        }
        Corso corso = maybeCorso.get();

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

        // 3) Doppia prenotazione
        boolean esiste = prenotazioneRepository.existsByUtenteAndCorso(utente, corso);
        if (esiste) {
            redirectAttrs.addFlashAttribute("warning", "Sei già prenotato a questo corso.");
            return "redirect:/corsi/" + corsoId;
        }

        // 4) Capienza
        int prenotati = prenotazioneRepository.countByCorso(corso);
        if (prenotati >= corso.getMaxPosti()) {
            redirectAttrs.addFlashAttribute("error", "Corso al completo. Non è possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }

        // 5) Salva
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
