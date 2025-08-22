package it.palestra.prenotazioni_palestra.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.palestra.prenotazioni_palestra.model.Utente;

public interface UtenteRepository extends JpaRepository<Utente, Integer> {

    Optional<Utente> findByEmail(String email); // utile per login in futuro

}
