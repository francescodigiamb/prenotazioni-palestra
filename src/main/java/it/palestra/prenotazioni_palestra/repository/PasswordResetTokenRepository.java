package it.palestra.prenotazioni_palestra.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.palestra.prenotazioni_palestra.model.PasswordResetToken;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByExpiresAtBefore(LocalDateTime time);
}
