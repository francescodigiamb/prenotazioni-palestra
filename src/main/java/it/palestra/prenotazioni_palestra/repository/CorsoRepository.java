package it.palestra.prenotazioni_palestra.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.palestra.prenotazioni_palestra.model.Corso;
import jakarta.persistence.LockModeType;

public interface CorsoRepository extends JpaRepository<Corso, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Corso c where c.id = :id")
    Optional<Corso> lockById(@Param("id") Integer id);
}
