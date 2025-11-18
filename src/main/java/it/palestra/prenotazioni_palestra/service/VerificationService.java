package it.palestra.prenotazioni_palestra.service;

import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.model.VerificationToken;
import it.palestra.prenotazioni_palestra.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationService {

    private final JavaMailSender mailSender;
    private final VerificationTokenRepository tokenRepo;

    @Value("${app.base-url}")
    private String baseUrl;

    public VerificationService(JavaMailSender mailSender, VerificationTokenRepository tokenRepo) {
        this.mailSender = mailSender;
        this.tokenRepo = tokenRepo;
    }

    @Transactional
    public void sendVerification(Utente utente) {
        // 60 minuti validità (cambia a piacere)
        var token = VerificationToken.of(utente, 60);
        tokenRepo.save(token);

        String link = baseUrl + "/verify?token=" + token.getToken();

        var msg = new SimpleMailMessage();
        msg.setTo(utente.getEmail());
        msg.setSubject("Conferma registrazione - FitnessClub");
        msg.setText("""
                Ciao %s,

                grazie per esserti registrato a FitnessClub!
                Conferma il tuo account cliccando il link:
                %s

                Il link scade tra 60 minuti.

                Se non sei stato tu a richiedere la registrazione, ignora questa email.
                """.formatted(utente.getNome() != null ? utente.getNome() : "", link));

        mailSender.send(msg);
    }
}
