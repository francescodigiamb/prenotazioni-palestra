package it.palestra.prenotazioni_palestra.service;

import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.model.VerificationToken;
import it.palestra.prenotazioni_palestra.repository.VerificationTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class VerificationService {

    private final VerificationTokenRepository tokenRepo;

    @Value("${app.base-url}")
    private String baseUrl;

    public VerificationService(VerificationTokenRepository tokenRepo) {
        this.tokenRepo = tokenRepo;
    }

    public void sendVerification(Utente utente) {
        // 1) crea token valido 60 minuti
        VerificationToken token = VerificationToken.of(utente, 60);
        tokenRepo.save(token);

        // 2) costruisci link
        String link = baseUrl + "/verify?token=" + token.getToken();

        // 3) per ora NON inviamo mail, logghiamo solo il link
        System.out.println("[VERIFICA] Link di verifica per " + utente.getEmail() + ": " + link);
    }
}
