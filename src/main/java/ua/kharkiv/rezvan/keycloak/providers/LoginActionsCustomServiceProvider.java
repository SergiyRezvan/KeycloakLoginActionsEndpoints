package ua.kharkiv.rezvan.keycloak.providers;

import ua.kharkiv.rezvan.keycloak.resources.LoginActionsCustomServiceResource;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class LoginActionsCustomServiceProvider implements RealmResourceProvider {


    private KeycloakSession session;

    public LoginActionsCustomServiceProvider(KeycloakSession session) {
        this.session = session;
    }

    public Object getResource() {
        return new LoginActionsCustomServiceResource(session);
    }

    public void close() {

    }
}
