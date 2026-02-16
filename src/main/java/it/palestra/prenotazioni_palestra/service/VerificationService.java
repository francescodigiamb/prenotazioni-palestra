package it.palestra.prenotazioni_palestra.service;

import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.model.VerificationToken;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import it.palestra.prenotazioni_palestra.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    private final VerificationTokenRepository tokenRepo;

    // Vecchio invio SMTP (lo teniamo commentato per future alternative)
    // private final EmailService emailService;

    // Nuovo invio via Brevo API (HTTP)
    private final BrevoEmailService brevoEmailService;

    private final String baseUrl;
    private final String appMailFrom;
    private final UtenteRepository utenteRepo;

    public VerificationService(
            VerificationTokenRepository tokenRepo,
            // EmailService emailService,
            BrevoEmailService brevoEmailService,
            @Value("${app.base-url}") String baseUrl,
            @Value("${app.mail.from}") String appMailFrom, UtenteRepository utenteRepo) {
        this.tokenRepo = tokenRepo;
        // this.emailService = emailService;
        this.brevoEmailService = brevoEmailService;
        this.baseUrl = baseUrl;
        this.appMailFrom = appMailFrom;
        this.utenteRepo = utenteRepo;
    }

    public void sendVerification(Utente utente) {

        // 1) crea token valido 60 minuti
        VerificationToken token = VerificationToken.of(utente, 60);
        tokenRepo.save(token);

        // 2) costruisci link
        String link = baseUrl + "/verify?token=" + token.getToken();

        // 3) prepara email
        String subject = "Conferma la registrazione – FitnessClub Chieti";

        // Vecchia versione testo (SMTP)
        // String body = "Ciao " + utente.getNome() + ",\n\n"
        // + "la registrazione è stata creata con successo.\n"
        // + "Per attivare l’account clicca sul link qui sotto:\n\n"
        // + link + "\n\n"
        // + "Email registrata: " + utente.getEmail() + "\n"
        // + "Il link scade tra 60 minuti.\n\n"
        // + "A presto,\nFitnessClub";

        String html = """
                <p>Ciao %s,</p>
                <p>grazie per esserti registrato su <b>FitnessClub Chieti</b>.</p>
                <p>Per attivare il tuo account clicca qui:</p>
                <p><a href="%s">Conferma email</a></p>
                <p>Il link scade tra 60 minuti.</p>
                <p>Se non hai richiesto tu la registrazione, ignora questa email.</p>
                <p>A presto,<br>FitnessClub Chieti</p>
                """.formatted(utente.getNome(), link);

        // 4) invia

        // SMTP (vecchio)
        // emailService.inviaEmailSemplice(utente.getEmail(), subject, body);

        // Brevo API (nuovo)
        brevoEmailService.sendVerificationEmail(appMailFrom, utente.getEmail(), subject, html);
    }

    public void reinviaVerifica(String email) {

        // 1) cerca utente
        Utente utente = utenteRepo.findByEmail(email).orElse(null);
        if (utente == null)
            return;

        // 2) se già attivo/verificato non fare nulla
        if (utente.isEnabled())
            return;

        // 3) (opzionale ma consigliato) elimina token precedenti non usati dell’utente
        tokenRepo.deleteAllByUtente_Id(utente.getId());

        // 4) rigenera e invia
        sendVerification(utente);
    }

}
