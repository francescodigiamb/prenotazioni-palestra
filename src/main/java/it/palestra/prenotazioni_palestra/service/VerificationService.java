package it.palestra.prenotazioni_palestra.service;

import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.model.VerificationToken;
import it.palestra.prenotazioni_palestra.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    private final VerificationTokenRepository tokenRepo;
    private final EmailService emailService;

    @Value("${app.base-url}")
    private String baseUrl;

    public VerificationService(VerificationTokenRepository tokenRepo,
            EmailService emailService,
            @Value("${app.base-url}") String baseUrl) {
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.baseUrl = baseUrl;
    }

    public void sendVerification(Utente utente) {

        // 1) crea token valido 60 minuti
        VerificationToken token = VerificationToken.of(utente, 60);
        tokenRepo.save(token);

        // 2) costruisci link
        String link = baseUrl + "/verify?token=" + token.getToken();

        // 3) prepara email
        String subject = "Completa la registrazione - FitnessClub";

        String body = "Ciao " + utente.getNome() + ",\n\n"
                + "la registrazione è stata creata con successo.\n"
                + "Per attivare l’account clicca sul link qui sotto:\n\n"
                + link + "\n\n"
                + "Email registrata: " + utente.getEmail() + "\n"
                + "Il link scade tra 60 minuti.\n\n"
                + "A presto,\nFitnessClub";

        // 4) invia
        emailService.inviaEmailSemplice(utente.getEmail(), subject, body);
    }

}
