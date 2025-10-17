package it.palestra.prenotazioni_palestra.security;

import it.palestra.prenotazioni_palestra.model.Utente;
import it.palestra.prenotazioni_palestra.repository.UtenteRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UtenteRepository utenteRepository;

    // costruttore
    public CustomUserDetailsService(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utente u = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email non trovata"));
        return new CustomUserDetails(u);
    }
}
