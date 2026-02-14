package it.palestra.prenotazioni_palestra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BrevoConfig {

    @Bean
    RestClient brevoRestClient(
            @Value("${brevo.api.url}") String url,
            @Value("${BREVO_API_KEY}") String apiKey) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
