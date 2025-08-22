// package it.palestra.prenotazioni_palestra.security;

// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.http.HttpMethod;
// import
// org.springframework.security.authentication.dao.DaoAuthenticationProvider;
// import
// org.springframework.security.config.annotation.web.builders.HttpSecurity;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
// import org.springframework.security.crypto.factory.PasswordEncoderFactories;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.web.SecurityFilterChain;

// @Configuration
// public class SecurityConfiguration {

// @Bean
// public PasswordEncoder passwordEncoder() {
// return new BCryptPasswordEncoder();
// }

// @Bean
// public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
// http
// .authorizeRequests()
// .antMatchers("/", "/login", "/register", "/css/**", "/js/**").permitAll()
// .antMatchers(HttpMethod.POST, "/dashboard/create", "/dashboard/edit/**",
// "/dashboard/delete/**")
// .hasAuthority("ADMIN")
// .antMatchers("/dashboard/create").hasAuthority("ADMIN")
// .antMatchers("/dashboard", "/dashboard/**").hasAnyAuthority("OPERATOR",
// "ADMIN")
// .anyRequest().authenticated()
// .and()
// .formLogin()
// .loginPage("/login")
// .permitAll()
// .and()
// .logout()
// .logoutUrl("/logout")
// .logoutSuccessUrl("/")
// .permitAll();

// return http.build();
// }
// }
