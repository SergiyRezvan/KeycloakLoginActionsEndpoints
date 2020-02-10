FROM jboss/keycloak:8.0.0

#add admin user
#RUN /opt/jboss/keycloak/bin/add-user-keycloak.sh -u admin -p P@asswrd --realm admin

COPY target/KeycloakLoginActionsEndpoints-1.0.jar /opt/jboss/keycloak/standalone/deployments

EXPOSE 8080

ENTRYPOINT [ "/opt/jboss/tools/docker-entrypoint.sh" ]