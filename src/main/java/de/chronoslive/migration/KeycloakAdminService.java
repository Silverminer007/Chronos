package de.chronoslive.migration;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final String keycloakRealm;

    public KeycloakAdminService(@Value("${de.chronos_live.migration.keycloak.base_url}") String keycloakBaseUrl,
                                @Value("${de.chronos_live.migration.keycloak.realm}") String realm,
                                @Value("${de.chronos_live.migration.keycloak.client_id}") String clientId,
                                @Value("${de.chronos_live.migration.keycloak.client_secret}") String clientSecret) {
        this.keycloakRealm = realm;
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakBaseUrl)  // Root URL!
                .realm(realm)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .build();
    }

    public Keycloak getClient() {
        return keycloak;
    }

    public List<UserRepresentation> listUsers() {
        return this.getClient().realm(this.keycloakRealm).users().list();
    }
}
