package it.palestra.prenotazioni_palestra.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.palestra.prenotazioni_palestra.model.Corso;
import it.palestra.prenotazioni_palestra.model.ModelloCorso;
import jakarta.persistence.LockModeType;

public interface CorsoRepository extends JpaRepository<Corso, Integer> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Corso c where c.id = :id")
    Optional<Corso> lockById(@Param("id") Integer id);

    // Questo serve per non creare doppioni (stesso modello, stessa data, stesso
    // orario)
    boolean existsByDataAndOrarioAndModello(LocalDate data, LocalTime orario, ModelloCorso modello);

    // Corsi tra due date (incluse), gia' ordinati; il modello viene caricato nella stessa query
    @Query("select c from Corso c left join fetch c.modello where c.data between :da and :a order by c.data asc, c.orario asc")
    List<Corso> findTraDate(@Param("da") LocalDate da, @Param("a") LocalDate a);
}
