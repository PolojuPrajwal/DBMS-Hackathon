package com.smartparking.security;

import com.smartparking.repo.UserRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/** HTTP Basic + DB users + role rules. ADMIN = operator (gates, dashboard, setup); USER = driver (reserve). */
@Configuration @EnableWebSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean UserDetailsService userDetailsService(UserRepository users) {
        return name -> users.findByUsername(name)
            .map(u -> User.withUsername(u.getUsername()).password(u.getPassword()).roles(u.getRole()).build())
            .orElseThrow(() -> new UsernameNotFoundException(name));
    }

    @Bean SecurityFilterChain chain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable()).cors(Customizer.withDefaults())
            .authorizeHttpRequests(a -> a
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/admin/**", "/api/records", "/api/entry", "/api/exit/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/locations", "/api/slots").hasRole("ADMIN")
                .anyRequest().authenticated())
            .httpBasic(b -> b.authenticationEntryPoint((req, res, e) -> res.sendError(401, "Invalid credentials")));
        return http.build();
    }
}
