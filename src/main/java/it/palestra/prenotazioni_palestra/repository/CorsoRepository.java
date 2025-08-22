package it.palestra.prenotazioni_palestra.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.palestra.prenotazioni_palestra.model.Corso;

public interface CorsoRepository extends JpaRepository<Corso, Integer> {

}
