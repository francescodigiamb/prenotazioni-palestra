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

    public void sendEmail(String fromEmail, String toEmail, String subject, String htmlContent) {
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

    public void inviaPromozioneDaRiserva(String fromEmail, String toEmail, String nomeCorso, String data, String ora) {
        String subject = "Sei stato confermato - " + nomeCorso;

        String html = """
                <p>Ciao,</p>
                <p>Si è liberato un posto e la tua prenotazione è stata confermata.</p>
                <p><b>Corso:</b> %s</p>
                <p><b>Quando:</b> %s alle %s</p>
                <p>A presto,<br>FitnessClub Chieti</p>
                """.formatted(nomeCorso, data, ora);

        sendEmail(fromEmail, toEmail, subject, html);
    }

    public void inviaNotificaDisdettaAdmin(String fromEmail, String adminEmail,
            String corsoNome, String data, String ora,
            String utenteNome, String utenteCognome, String utenteEmail) {

        String subject = "Disdetta prenotazione - " + corsoNome;

        String html = """
                <p>È stata annullata una prenotazione.</p>
                <p><b>Corso:</b> %s</p>
                <p><b>Data:</b> %s</p>
                <p><b>Ora:</b> %s</p>
                <br>
                <p><b>Utente:</b> %s %s</p>
                """.formatted(corsoNome, data, ora, utenteNome, utenteCognome, utenteEmail);

        sendEmail(fromEmail, adminEmail, subject, html);
    }

}
