package it.palestra.prenotazioni_palestra.repository;

import it.palestra.prenotazioni_palestra.model.ModelloCorso;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelloCorsoRepository extends JpaRepository<ModelloCorso, Integer> {
}
