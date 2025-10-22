package it.palestra.prenotazioni_palestra.config;

import it.palestra.prenotazioni_palestra.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

        private final CustomUserDetailsService userDetailsService;
        private final PasswordEncoder passwordEncoder;

        public SecurityConfig(CustomUserDetailsService userDetailsService,
                        PasswordEncoder passwordEncoder) {
                this.userDetailsService = userDetailsService;
                this.passwordEncoder = passwordEncoder;
        }

        @Bean
        public DaoAuthenticationProvider authProvider() {
                DaoAuthenticationProvider p = new DaoAuthenticationProvider();
                p.setUserDetailsService(userDetailsService);
                p.setPasswordEncoder(passwordEncoder);
                return p;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .authenticationProvider(authProvider())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/home", "/corsi", "/corsi/*", "/listino",
                                                                "/login",
                                                                "/register", "/css/**", "/js/**")
                                                .permitAll()
                                                .requestMatchers("/account/**").authenticated()
                                                .requestMatchers("/prenotazioni/**").authenticated()
                                                .requestMatchers("/admin/**").hasRole("ADMIN")
                                                .anyRequest().permitAll())

                                .formLogin(form -> form
                                                .loginPage("/login").permitAll()
                                                .defaultSuccessUrl("/corsi", true))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/home?logout")
                                                .invalidateHttpSession(true)
                                                .deleteCookies("JSESSIONID")
                                                .permitAll())
                                .csrf(Customizer.withDefaults());

                return http.build();
        }
}
