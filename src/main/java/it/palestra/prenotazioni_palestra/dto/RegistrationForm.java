package it.palestra.prenotazioni_palestra.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegistrationForm {
    @NotBlank(message = "Il nome è obbligatorio")
    private String nome;

    @NotBlank
    @Email(message = "Email non valida")
    private String email;

    @NotBlank
    @Size(min = 6, message = "Minimo 6 caratteri")
    private String password;

    @NotBlank
    private String confermaPassword;

    // GETTER-SETTER
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfermaPassword() {
        return confermaPassword;
    }

    public void setConfermaPassword(String confermaPassword) {
        this.confermaPassword = confermaPassword;
    }

}
