package it.palestra.prenotazioni_palestra.service;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BrevoEmailService {

    private final RestClient brevoRestClient;

    public BrevoEmailService(RestClient brevoRestClient) {
        this.brevoRestClient = brevoRestClient;
    }

    public void sendVerificationEmail(String fromEmail, String toEmail, String subject, String htmlContent) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromEmail, "name", "FitnessClub Chieti"),
                "to", new Object[] { Map.of("email", toEmail) },
                "subject", subject,
                "htmlContent", htmlContent);

        brevoRestClient.post()
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }
}
