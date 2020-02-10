package ua.kharkiv.rezvan.keycloak.resources;

import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.common.ClientConnection;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.SystemClientUtil;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilderException;
import java.util.Objects;

public class LoginActionsCustomServiceResource {

    private static final String REGISTRATION_NOT_ALLOWED = "Registration not allowed";
    private static final String CLIENT_ID_MISSED = "Registration not allowed. Client id is not specified";
    private static final String CLIENT_NOT_FOUND = "Client not found.";
    private KeycloakSession session;

    private RealmModel realm;

    private EventBuilder event;

    private ClientConnection clientConnection;

    private AuthenticationProcessor authenticationProcessor;

    public LoginActionsCustomServiceResource(KeycloakSession session) {
        this.session = session;
        this.clientConnection = session.getContext().getConnection();
        this.realm = session.getContext().getRealm();
        this.event = new EventBuilder(session.getContext().getRealm(), session, clientConnection);
        this.authenticationProcessor = new CustomAuthenticationProcessor();
    }

    @Path("userRegistration")
    @POST
    @Produces("application/json")
    public Response registerUser(@QueryParam("clientId") String clientId, @Context HttpRequest request) {

        event.event(EventType.REGISTER);

        if (!realm.isRegistrationAllowed()) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorResponse.error(REGISTRATION_NOT_ALLOWED, Response.Status.BAD_REQUEST);
        }

        if (Objects.isNull(clientId) || clientId.trim().length() == 0) {
            event.error(Errors.REGISTRATION_DISABLED);
            return ErrorResponse.error(CLIENT_ID_MISSED, Response.Status.BAD_REQUEST);
        }

        ClientModel realmClient = realm.getClientByClientId(clientId);
        if (realmClient == null) {
            event.error(Errors.CLIENT_NOT_FOUND);
            return ErrorResponse.error(CLIENT_NOT_FOUND, Response.Status.BAD_REQUEST);
        }

        event.client(realmClient);
        session.getContext().setClient(realmClient);

        AuthenticationFlowModel registrationFlow = realm.getRegistrationFlow();
        AuthenticationSessionModel authSession = createAuthenticationSessionForClient();

        authenticationProcessor.setAuthenticationSession(authSession)
                .setFlowPath("registration")
                .setBrowserFlow(false)
                .setFlowId(registrationFlow.getId())
                .setConnection(clientConnection)
                .setEventBuilder(event)
                .setRealm(realm)
                .setSession(session)
                .setUriInfo(session.getContext().getUri())
                .setRequest(request);

        String registrationFlowId = registrationFlow.getId();

        String execution = realm.getAuthenticationExecutions(registrationFlowId).get(0).getId();

        return authenticationProcessor.authenticationAction(execution);
    }

    private AuthenticationSessionModel createAuthenticationSessionForClient()
            throws UriBuilderException, IllegalArgumentException {
        AuthenticationSessionModel authSession;

        // set up the account service as the endpoint to call.
        ClientModel client = SystemClientUtil.getSystemClient(realm);

        RootAuthenticationSessionModel rootAuthSession = new AuthenticationSessionManager(session).createAuthenticationSession(realm, false);
        authSession = rootAuthSession.createAuthenticationSession(client);

        authSession.setAction(AuthenticationSessionModel.Action.AUTHENTICATE.name());
        //authSession.setNote(AuthenticationManager.END_AFTER_REQUIRED_ACTIONS, "true");
        authSession.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        String redirectUri = Urls.accountBase(session.getContext().getUri().getBaseUri()).path("/").build(realm.getName()).toString();
        authSession.setRedirectUri(redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.RESPONSE_TYPE_PARAM, OAuth2Constants.CODE);
        authSession.setClientNote(OIDCLoginProtocol.REDIRECT_URI_PARAM, redirectUri);
        authSession.setClientNote(OIDCLoginProtocol.ISSUER, Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

        return authSession;
    }
}
