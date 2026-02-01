package it.palestra.prenotazioni_palestra.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void inviaEmailSemplice(String to, String subject, String body) {

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(from);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);

        mailSender.send(msg);
    }

    public void inviaEmailTest(String to) {
        inviaEmailSemplice(
                to,
                "Test Email - FitnessClub",
                "Ciao! Questa è una mail di test inviata dal progetto Prenotazioni Palestra.");
    }

    @Value("${app.mail.admin}")
    private String adminEmail;

    public void inviaConfermaRegistrazione(String to, String nome) {
        String subject = "Registrazione completata - FitnessClub";
        String body = "Ciao " + nome + ",\n\n"
                + "la tua registrazione è avvenuta con successo.\n\n"
                + "A presto,\nFitnessClub";
        inviaEmailSemplice(to, subject, body);
    }

    public void inviaNotificaDisdettaAdmin(String corsoNome, String data, String ora, String utenteNome,
            String utenteCognome, String utenteEmail) {
        String subject = "Disdetta prenotazione - " + corsoNome;
        String body = "È stata annullata una prenotazione.\n\n"
                + "Corso: " + corsoNome + "\n"
                + "Data: " + data + "\n"
                + "Ora: " + ora + "\n\n"
                + "Utente: " + utenteNome + " " + utenteCognome + "\n"
                + "Email: " + utenteEmail + "\n";
        inviaEmailSemplice(adminEmail, subject, body);
    }

    public void inviaPromozioneDaRiserva(String to, String nomeCorso, String data, String ora) {
        String subject = "Sei stato confermato - " + nomeCorso;
        String body = "Ciao,\n\n"
                + "si è liberato un posto e la tua prenotazione è stata confermata.\n"
                + "Corso: " + nomeCorso + "\n"
                + "Quando: " + data + " alle " + ora + "\n\n"
                + "A presto,\nFitnessClub";
        inviaEmailSemplice(to, subject, body);
    }

}
