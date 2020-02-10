package ua.kharkiv.rezvan.keycloak.resources;

import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.AuthenticationProcessor;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.ErrorResponse;

import javax.ws.rs.core.Response;
import java.util.stream.Collectors;

public class CustomAuthenticationProcessor extends AuthenticationProcessor {

    private static final String FAILED_REGISTRATION_MESSAGE = "Could not register a user. Please contact admin.";

    public CustomAuthenticationProcessor() {
        super();
    }

    @Override
    public Response authenticationAction(String execution) {
        logger.debug("Custom authentication action");
        AuthenticationExecutionModel model = realm.getAuthenticationExecutionById(execution);
        if (model == null) {
            logger.warn("Cannot find execution for registration.");
            logFailure();
            resetFlow();
            return ErrorResponse.error(FAILED_REGISTRATION_MESSAGE, Response.Status.INTERNAL_SERVER_ERROR);
        }
        client = session.getContext().getClient();
        event.client(authenticationSession.getClient().getClientId())
                .detail(Details.REDIRECT_URI, authenticationSession.getRedirectUri())
                .detail(Details.AUTH_METHOD, authenticationSession.getProtocol());
        String authType = authenticationSession.getAuthNote(Details.AUTH_TYPE);
        if (authType != null) {
            event.detail(Details.AUTH_TYPE, authType);
        }

        AuthenticationFlow authenticationFlow = createFlowExecution(this.flowId, model);
        authenticationFlow.processAction(execution);
        UserModel authenticatedUser = authenticationSession.getAuthenticatedUser();
        if (authenticatedUser == null) {
            logger.warn("User session was not created after registration. Something went wrong.");
            return ErrorResponse.error(FAILED_REGISTRATION_MESSAGE, Response.Status.INTERNAL_SERVER_ERROR);
        }
        if (!authenticationFlow.isSuccessful()) {
            String errors = authenticationFlow.getFlowExceptions()
                    .stream()
                    .map(ex -> ex.getError().toString())
                    .collect(Collectors.joining(", "));
            logger.error("User was not registered due to following errors: " + errors);
            return ErrorResponse.error(FAILED_REGISTRATION_MESSAGE + ". Details = " + errors, Response.Status.INTERNAL_SERVER_ERROR);
        }

        return Response.status(201).build();
    }
}
