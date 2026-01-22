package de.chronoslive.migration.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import reactor.core.publisher.Mono;

@Configuration
public class ClientConfig {
    private static final Logger log = LoggerFactory.getLogger(ClientConfig.class);

    @Value("${de.chronos_live.migration.target_api_url}")
    private String baseUrl;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(baseUrl)
                .filter(tokenRelayFilter())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("Response Status: {}", response.statusCode());
            log.info("Content-Type: {}", response.headers().contentType());
            return Mono.just(response);
        });
    }

    private ExchangeFilterFunction tokenRelayFilter() {
        return (request, next) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            log.info("Auth type: {}", auth != null ? auth.getClass().getSimpleName() : "null");

            if (auth instanceof JwtAuthenticationToken jwt) {
                log.info("Token found, adding to request");
                ClientRequest filtered = ClientRequest.from(request)
                        .headers(h -> h.setBearerAuth(jwt.getToken().getTokenValue()))
                        .build();
                return next.exchange(filtered);
            } else {
                log.warn("No JWT token in SecurityContext!");
            }
            return next.exchange(request);
        };
    }

    private <T> T createClient(WebClient webClient, Class<T> clientClass) {
        return HttpServiceProxyFactory
                .builderFor(WebClientAdapter.create(webClient))
                .build()
                .createClient(clientClass);
    }

    @Bean
    public UserClient userClient(WebClient webClient) {
        return createClient(webClient, UserClient.class);
    }

    @Bean
    public FriendshipClient friendshipClient(WebClient webClient) {
        return createClient(webClient, FriendshipClient.class);
    }

    @Bean
    public GroupClient groupClient(WebClient webClient) {
        return createClient(webClient, GroupClient.class);
    }

    @Bean
    public AppointmentClient appointmentClient(WebClient webClient) {
        return createClient(webClient, AppointmentClient.class);
    }
}