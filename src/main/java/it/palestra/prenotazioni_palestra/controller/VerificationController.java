package it.palestra.prenotazioni_palestra.controller;

import it.palestra.prenotazioni_palestra.model.VerificationToken;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import it.palestra.prenotazioni_palestra.repository.VerificationTokenRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;

import java.time.LocalDateTime;

@Controller
public class VerificationController {

    private final VerificationTokenRepository tokenRepo;
    private final UtenteRepository utenteRepo;

    public VerificationController(VerificationTokenRepository tokenRepo, UtenteRepository utenteRepo) {
        this.tokenRepo = tokenRepo;
        this.utenteRepo = utenteRepo;
    }

    @GetMapping("/verify")
    public String verify(String token, Model model) {
        var maybe = tokenRepo.findByToken(token);
        if (maybe.isEmpty()) {
            model.addAttribute("error", "Token non valido.");
            return "verify-result";
        }
        VerificationToken vt = maybe.get();

        if (vt.isUsed()) {
            model.addAttribute("warning", "Token già utilizzato. Il tuo account potrebbe essere già attivo.");
            return "verify-result";
        }
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "Token scaduto. Richiedi una nuova verifica.");
            return "verify-result";
        }

        // abilita utente
        var u = vt.getUtente();
        u.setEnabled(true);
        utenteRepo.save(u);

        // marca token usato
        vt.setUsed(true);
        tokenRepo.save(vt);

        model.addAttribute("success", "Account verificato! Ora puoi accedere.");
        return "verify-result";
    }
}
