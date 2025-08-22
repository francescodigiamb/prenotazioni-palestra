package it.palestra.prenotazioni_palestra.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.palestra.prenotazioni_palestra.model.Prenotazione;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Integer> {
    List<Prenotazione> findByUtenteId(Integer utenteId);

    List<Prenotazione> findByCorsoId(Integer corsoId);
}
