# KeycloakLoginActionsEndpoints
Keycloak extension that allows to register users in Keycloak for specified Realm and existing client via REST.

By default, we can register user in Keycloak only via Keycloak UI. Default registration endpoint requires browser data, some cookies and so on. It's not possible to use in microservice environment.

In order to solve this problem, the project extends the Keycloak LoginActions endpoints and with this implementation we can hide usage of Keycloak. 
For example, we have some API for registering users. It requires two parameters - email and password. Backend service receive the request, calls Keycloak to register a user and Keycloak returns Http status 201 that user was registered successfully. Then user can login, backend service can call Keycloak endpoint to get an authorization token.

The project contains docker-compose and Docker files in order to test it quickly.

In order to start application please execute:
 
`mvn clean package && docker-compose up`

Once server is started, you can register a client with next endpoint:

`curl --location --request POST 'http://localhost:8180/auth/realms/{REALM}/loginActions/userRegistration?clientId={CLIENT_NAME}' \
 --header 'Content-Type: application/x-www-form-urlencoded' \
 --data-urlencode 'email=new_user@domain.com' \
 --data-urlencode 'password=password' \
 --data-urlencode 'password-confirm=password'`

**NOTE:** In order to register a user, registration should be allowed on specified realm.

Feel free to ask any questions or some additional features.