package de.chronoslive.security;

import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfiguration {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    /**
     * 1️⃣ API-Bereich: stateless, JWT, kein CSRF
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // nur für API-Pfade
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/ical/organisation/*/public.ics").permitAll()
                        .requestMatchers("/api/v1/ical/public.ics").permitAll()
                        .requestMatchers("/api/v1/**").hasRole("ADMIN_API")
                        .anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable) // stateless -> kein CSRF
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())
                ));

        return http.build();
    }

    /**
     * 2️⃣ UI-Bereich: Vaadin mit Session, CSRF, OIDC-Login
     */
    @Bean
    @Order(2)
    public SecurityFilterChain vaadinSecurity(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/**").permitAll()
                )
                .with(VaadinSecurityConfigurer.vaadin(), vaadin -> {
                })
                .oauth2Login(Customizer.withDefaults())
                .logout(logout -> {
                    var handler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
                    handler.setPostLogoutRedirectUri("{baseUrl}/");
                    logout.logoutSuccessHandler(handler);
                });

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Map<String, Object> realmAccess = jwt.getClaim("realm_access");
            if (realmAccess != null) {
                var roles = (Collection<String>) realmAccess.get("roles");
                authorities.addAll(roles.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
                        .toList());
            }
            return authorities;
        });
        return converter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}