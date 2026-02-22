package it.palestra.prenotazioni_palestra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class BrevoConfig {

    @Bean
    @ConditionalOnProperty(name = "brevo.api.key") // crea il bean SOLO se la key è presente e non vuota
    RestClient brevoRestClient(
            @Value("${brevo.api.url}") String url,
            @Value("${brevo.api.key}") String apiKey) {
        return RestClient.builder()
                .baseUrl(url)
                .defaultHeader("api-key", apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
