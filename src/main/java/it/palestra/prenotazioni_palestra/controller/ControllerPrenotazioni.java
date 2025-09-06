package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.model.Prenotazione;
import it.palestra.prenotazioni_palestra.repository.CorsoRepository;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import it.palestra.prenotazioni_palestra.repository.PrenotazioneRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class ControllerPrenotazioni {

    @Autowired
    private CorsoRepository corsoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    /**
     * Gestisce la creazione di una prenotazione.
     * Passi:
     * 1) Recupero corso (obbligatorio)
     * 2) Se utente con email esiste -> uso quello, altrimenti creo utente base
     * 3) Verifico doppia prenotazione (utente+corso)
     * 4) Verifico capienza (posti già prenotati < maxPosti)
     * 5) Salvo Prenotazione e redirect al dettaglio corso con messaggio
     */
    @PostMapping("/prenotazioni")
    public String creaPrenotazione(@RequestParam("corsoId") Integer corsoId,
            @RequestParam("email") String email,
            @RequestParam("nome") String nome,
            RedirectAttributes redirectAttrs) {

        // 1) Recupero corso
        Optional<Corso> nomeCorso = corsoRepository.findById(corsoId);
        if (!nomeCorso.isPresent()) {
            // Se il corso non esiste, torno alla lista con un messaggio di errore
            redirectAttrs.addFlashAttribute("error", "Corso non trovato.");
            return "redirect:/corsi";
        }
        Corso corso = nomeCorso.get();

        // 2) Recupero (o creo) l'utente

        Utente utente;
        Optional<Utente> maybeUtente = utenteRepository.findByEmail(email);
        if (maybeUtente.isPresent()) {
            utente = maybeUtente.get();
            // Se l'utente esiste ma non ha il nome aggiornato, lo aggiorno (facoltativo)
            if (nome != null && !nome.trim().isEmpty()
                    && (utente.getNome() == null || !utente.getNome().equals(nome))) {
                utente.setNome(nome);
                utenteRepository.save(utente);
            }
        } else {
            // Creo un nuovo utente "base": ruolo UTENTE, password temporanea
            utente = new Utente();
            utente.setNome(nome);
            utente.setEmail(email);
            utente.setRuolo("UTENTE");
            // Password provvisoria (rispetta @Size(min=6)); la sistemeremo quando
            // aggiungiamo la registrazione/login veri
            utente.setPassword("123456");
            utente = utenteRepository.save(utente);
        }

        // 3) Evita doppie prenotazioni dello stesso utente per lo stesso corso
        boolean esiste = prenotazioneRepository.existsByUtenteAndCorso(utente, corso);
        if (esiste) {
            redirectAttrs.addFlashAttribute("warning", "Sei già prenotato a questo corso.");
            return "redirect:/corsi/" + corsoId;
        }

        // 4) Evita overbooking (se già pieno)
        int prenotati = prenotazioneRepository.countByCorso(corso);
        if (prenotati >= corso.getMaxPosti()) {
            redirectAttrs.addFlashAttribute("error", "Corso al completo. Non è possibile prenotare.");
            return "redirect:/corsi/" + corsoId;
        }

        // 5) Salvataggio prenotazione
        Prenotazione p = new Prenotazione(utente, corso);
        prenotazioneRepository.save(p);

        redirectAttrs.addFlashAttribute("success", "Prenotazione effettuata con successo!");
        return "redirect:/corsi/" + corsoId;
    }
}
