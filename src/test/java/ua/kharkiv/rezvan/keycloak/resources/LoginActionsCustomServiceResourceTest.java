package ua.kharkiv.rezvan.keycloak.resources;

import org.jboss.resteasy.spi.HttpRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.common.ClientConnection;
import org.keycloak.models.*;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LoginActionsCustomServiceResourceTest {

    public static final String CLIENT_ID = "2387462398764";
    @Mock
    private KeycloakSession session;

    @Mock
    private KeycloakContext keycloakContext;

    @Mock
    private ClientConnection clientConnection;

    @Mock
    private RealmModel realmModel;

    @Mock
    private HttpRequest request;

    @Mock
    private ClientModel clientModel;

    @Mock
    private AuthenticationFlowModel registrationFlow;

    @Mock
    private ClientModel systemClient;

    @Mock
    private AuthenticationSessionProvider authenticationSessionProvider;

    @Mock
    private RootAuthenticationSessionModel rootAuthenticationSessionModel;

    private LoginActionsCustomServiceResource loginActionsCustomServiceResource;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(keycloakContext.getRealm()).thenReturn(realmModel);
        when(realmModel.getName()).thenReturn("admin");
        when(realmModel.getRegistrationFlow()).thenReturn(registrationFlow);
        when(registrationFlow.getId()).thenReturn("registerId");
        when(realmModel.isRegistrationAllowed()).thenReturn(true);
        when(keycloakContext.getConnection()).thenReturn(clientConnection);
        when(session.getContext()).thenReturn(keycloakContext);
        when(session.getContext().getClient()).thenReturn(clientModel);
        when(clientModel.getClientId()).thenReturn("clientId");
        loginActionsCustomServiceResource = new LoginActionsCustomServiceResource(session);
    }

    @Test
    public void shouldReturnBadRequestInCaseClientIdIsNotSpecified() {
        Response response = loginActionsCustomServiceResource.registerUser(null, request);
        assertEquals(response.getStatus(), 400);
        ErrorRepresentation errorRepresentation = (ErrorRepresentation) response.getEntity();
        assertEquals(errorRepresentation.getErrorMessage(), "Registration not allowed. Client id is not specified");
    }

    @Test
    public void shouldReturnBadRequestInCaseRegistrationNotAllowed() {
        when(realmModel.isRegistrationAllowed()).thenReturn(false);
        Response response = loginActionsCustomServiceResource.registerUser(null, request);
        assertEquals(response.getStatus(), 400);
        ErrorRepresentation errorRepresentation = (ErrorRepresentation) response.getEntity();
        assertEquals(errorRepresentation.getErrorMessage(), "Registration not allowed");
    }

    @Test
    public void shouldReturnBadRequestInCaseClientIsNotFound() {
        Response response = loginActionsCustomServiceResource.registerUser(CLIENT_ID, request);
        assertEquals(response.getStatus(), 400);
        ErrorRepresentation errorRepresentation = (ErrorRepresentation) response.getEntity();
        assertEquals(errorRepresentation.getErrorMessage(), "Client not found.");
    }

    @Test
    public void shouldThrowAuthenticationFlowException() throws Exception {
        additionalInit();
        Response response = loginActionsCustomServiceResource.registerUser("2387462398764", request);
        assertEquals(response.getStatus(), 500);
        ErrorRepresentation errorRepresentation = (ErrorRepresentation) response.getEntity();
        assertEquals(errorRepresentation.getErrorMessage(), "Could not register a user. Please contact admin.");
    }

    private void additionalInit() throws Exception {
        when(realmModel.getClientByClientId(CLIENT_ID)).thenReturn(clientModel);
        when(realmModel.getClientByClientId("account")).thenReturn(systemClient);
        when(session.authenticationSessions()).thenReturn(authenticationSessionProvider);
        when(authenticationSessionProvider.createRootAuthenticationSession(realmModel)).thenReturn(rootAuthenticationSessionModel);
        AuthenticationSessionModel authSession = mock(AuthenticationSessionModel.class);
        when(authSession.getClient()).thenReturn(clientModel);
        when(rootAuthenticationSessionModel.createAuthenticationSession(systemClient)).thenReturn(authSession);
        KeycloakUriInfo keycloakUriInfo = mock(KeycloakUriInfo.class);
        when(keycloakUriInfo.getBaseUri()).thenReturn(new URI("http://localhost:8080/someUrl"));
        when(session.getContext().getUri()).thenReturn(keycloakUriInfo);

        List<AuthenticationExecutionModel> executionModelList = new ArrayList<>();
        AuthenticationExecutionModel authenticationExecutionModel = mock(AuthenticationExecutionModel.class);
        when(realmModel.getAuthenticationExecutions("registerId")).thenReturn(executionModelList);
        when(authenticationExecutionModel.getId()).thenReturn("execId");
        executionModelList.add(authenticationExecutionModel);
        when(realmModel.getAuthenticationExecutionById("execId")).thenReturn(authenticationExecutionModel);
        AuthenticationFlowModel model = mock(AuthenticationFlowModel.class);
        when(model.getProviderId()).thenReturn("form-flow");
        when(realmModel.getAuthenticationFlowById("registerId")).thenReturn(model);
    }

}
