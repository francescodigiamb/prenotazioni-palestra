package it.palestra.prenotazioni_palestra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PrenotazioniPalestraApplication {

	public static void main(String[] args) {
		SpringApplication.run(PrenotazioniPalestraApplication.class, args);
	}

}
