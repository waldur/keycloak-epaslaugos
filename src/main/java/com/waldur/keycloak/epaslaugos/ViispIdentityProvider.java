package com.waldur.keycloak.epaslaugos;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.*;
import java.util.UUID;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ViispIdentityProvider extends AbstractIdentityProvider<IdentityProviderModel> {

    public static final String PROVIDER_ID = "viisp";

    private final ViispXMLClient xmlClient;

    private static final Logger LOG = LoggerFactory.getLogger(ViispIdentityProvider.class);

    public ViispIdentityProvider(KeycloakSession session, ViispIdentityProviderConfig config) {
        super(session, config);
        this.xmlClient = new ViispXMLClient(config);
    }

    @Override
    public void updateBrokeredUser(
            KeycloakSession session,
            RealmModel realm,
            UserModel user,
            BrokeredIdentityContext context) {
        // Sync core identity fields on every login
        if (context.getFirstName() != null) {
            user.setFirstName(context.getFirstName());
        }
        if (context.getLastName() != null) {
            user.setLastName(context.getLastName());
        }
        if (context.getEmail() != null) {
            user.setEmail(context.getEmail());
        }

        LOG.info(
                "Updated user {} core fields: firstName={}, lastName={}, email={}",
                user.getUsername(),
                context.getFirstName(),
                context.getLastName(),
                context.getEmail());

        // Sync custom attributes
        syncAttribute(user, context, "lt-personal-code");
        syncAttribute(user, context, "schacPersonalUniqueID");
        syncAttribute(user, context, "authentication-provider");

        // Sync company attributes (clear when not present, i.e. individual login)
        syncCompanyAttribute(user, context, "companyName");
        syncCompanyAttribute(user, context, "companyCode");
    }

    private void syncAttribute(UserModel user, BrokeredIdentityContext context, String attrName) {
        String value = context.getUserAttribute(attrName);
        if (value != null && !value.isEmpty()) {
            user.setSingleAttribute(attrName, value);
        }
    }

    private void syncCompanyAttribute(
            UserModel user, BrokeredIdentityContext context, String attrName) {
        String value = context.getUserAttribute(attrName);
        if (value != null && !value.isEmpty()) {
            user.setSingleAttribute(attrName, value);
            LOG.info("Updated user {} {} to: {}", user.getUsername(), attrName, value);
        } else {
            user.removeAttribute(attrName);
            LOG.info("Cleared {} for user {} (not a company login)", attrName, user.getUsername());
        }
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new ViispEndpoint(this, callback, realm, event);
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        LOG.info("Started login process using VIISP IDP");
        if (request == null) {
            throw new IdentityBrokerException("Authentication request cannot be null");
        }

        AuthenticationSessionModel authSession = request.getAuthenticationSession();
        if (authSession == null) {
            throw new IdentityBrokerException("Authentication session is required");
        }

        try {
            // Store CSRF protection state in authentication session
            String viispState = UUID.randomUUID().toString();
            authSession.setAuthNote("VIISP_STATE", viispState);

            // Create encoded state using IdentityBrokerState encoding - this is what
            // parseSessionCode expects
            ClientSessionCode<AuthenticationSessionModel> clientSessionCode =
                    new ClientSessionCode<>(session, request.getRealm(), authSession);
            String relayState = clientSessionCode.getOrGenerateCode();
            String encodedState =
                    IdentityBrokerState.decoded(
                                    relayState,
                                    authSession.getClient().getId(),
                                    authSession.getClient().getClientId(),
                                    authSession.getTabId(),
                                    null)
                            .getEncoded();
            // Now create IdentityBrokerState.encoded() format which includes the compound
            // ID
            LOG.info("Compound ID encoded state: {}", encodedState);

            LOG.info("Auth session ID: {}", authSession.getParentSession().getId());
            LOG.info(
                    "Auth session client (client) ID: {}",
                    authSession.getClient() != null
                            ? authSession.getClient().getClientId()
                            : "null");
            LOG.info(
                    "Auth session client ID: {}",
                    authSession.getClient() != null ? authSession.getClient().getId() : "null");
            LOG.info("Auth session realm: {}", authSession.getRealm().getName());
            LOG.info("Auth session action: {}", authSession.getAction());
            LOG.info("Current time: {}", System.currentTimeMillis() / 1000);

            // Print all realm timeout settings
            RealmModel realm = authSession.getRealm();
            LOG.info("=== REALM TIMEOUT SETTINGS ===");
            LOG.info("SSO Session Max Lifespan: {} seconds", realm.getSsoSessionMaxLifespan());
            LOG.info("SSO Session Idle Timeout: {} seconds", realm.getSsoSessionIdleTimeout());
            LOG.info(
                    "SSO Session Max Lifespan Remember Me: {} seconds",
                    realm.getSsoSessionMaxLifespanRememberMe());
            LOG.info(
                    "SSO Session Idle Timeout Remember Me: {} seconds",
                    realm.getSsoSessionIdleTimeoutRememberMe());
            LOG.info(
                    "Client Session Idle Timeout: {} seconds", realm.getClientSessionIdleTimeout());
            LOG.info(
                    "Client Session Max Lifespan: {} seconds", realm.getClientSessionMaxLifespan());
            LOG.info("Access Token Lifespan: {} seconds", realm.getAccessTokenLifespan());
            LOG.info(
                    "Access Token Lifespan for Implicit Flow: {} seconds",
                    realm.getAccessTokenLifespanForImplicitFlow());
            LOG.info("Access Code Lifespan: {} seconds", realm.getAccessCodeLifespan());
            LOG.info(
                    "Access Code Lifespan User Action: {} seconds",
                    realm.getAccessCodeLifespanUserAction());
            LOG.info("Access Code Lifespan Login: {} seconds", realm.getAccessCodeLifespanLogin());
            LOG.info(
                    "Action Token Generated By Admin Lifespan: {} seconds",
                    realm.getActionTokenGeneratedByAdminLifespan());
            LOG.info(
                    "Action Token Generated By User Lifespan: {} seconds",
                    realm.getActionTokenGeneratedByUserLifespan());
            LOG.info("=== END REALM TIMEOUT SETTINGS ===");

            // Print client info (timeout settings are at realm level)
            if (authSession.getClient() != null) {
                LOG.info("=== CLIENT INFO ===");
                LOG.info("Client ID: {}", authSession.getClient().getClientId());
                LOG.info("Client Name: {}", authSession.getClient().getName());
                LOG.info("Client Protocol: {}", authSession.getClient().getProtocol());
                LOG.info("=== END CLIENT INFO ===");
            }

            // Use the default redirect URL
            String callbackUrl = request.getRedirectUri();
            LOG.info("Callback URL: {}", callbackUrl);

            // Initialize and validate VIISP configuration
            ViispIdentityProviderConfig viispConfig = new ViispIdentityProviderConfig(getConfig());
            validateConfig(viispConfig);

            // Request authentication ticket from VIISP
            String ticketId =
                    xmlClient.requestAuthenticationTicket(
                            callbackUrl,
                            viispConfig.getServiceId(),
                            viispConfig.getAuthServiceUrl(),
                            encodedState);
            if (ticketId == null || ticketId.trim().isEmpty()) {
                throw new IdentityBrokerException(
                        "Failed to obtain authentication ticket from VIISP");
            }
            authSession.setAuthNote("VIISP_TICKET_ID", ticketId);
            authSession.setAuthNote("VIISP_STATE_COPY", encodedState);
            authSession.setAuthNote("VIISP_TIMESTAMP", String.valueOf(System.currentTimeMillis()));
            LOG.info("Authentication session after setting notes: {}", authSession);
            LOG.info("Stored VIISP_TICKET_ID: {}", ticketId);
            LOG.info("Stored VIISP_STATE_COPY: {}", encodedState);
            String redirectUrl = viispConfig.getRedirectServiceUrl();
            LOG.info("Stored auth notes in session");
            LOG.info("Using the redirect service URL {}", redirectUrl);
            String ticketSubmitPage = createTicketSubmitPage(ticketId, redirectUrl);
            return Response.ok(ticketSubmitPage).type(MediaType.TEXT_HTML_TYPE).build();
        } catch (IllegalArgumentException e) {
            throw new IdentityBrokerException("Invalid VIISP configuration: " + e.getMessage(), e);
        } catch (java.io.IOException | InterruptedException e) {
            throw new IdentityBrokerException("Failed to communicate with VIISP service", e);
        } catch (IdentityBrokerException e) {
            // Re-throw IdentityBrokerException as-is
            throw e;
        } catch (Exception e) {
            throw new IdentityBrokerException("Unexpected error during VIISP authentication", e);
        }
    }

    private String createTicketSubmitPage(String ticketId, String redirectUrl) {
        return String.format(
                "<!DOCTYPE html>"
                        + "<html><head>"
                        + "<meta charset='utf-8'>"
                        + "<meta name='viewport' content='width=device-width, initial-scale=1'>"
                        + "<title>Auth to VIISP</title>"
                        + "<style>"
                        + "body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background-color: #f5f5f5; }"
                        + ".container { text-align: center; background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
                        + ".btn { background-color: #007bff; color: white; padding: 12px 24px; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }"
                        + ".btn:hover { background-color: #0056b3; }"
                        + "</style>"
                        + "<script>window.onload=function(){document.forms['REQUEST'].submit();}</script>"
                        + "</head>"
                        + "<body>"
                        + "<div class='container'>"
                        + "<h2>Redirecting to VIISP...</h2>"
                        + "<p>Please wait while we redirect you to the authentication service.</p>"
                        + "<form name='REQUEST' method='post' action='%s'>"
                        + "<input type='hidden' name='ticket' value='%s'/>"
                        + "<noscript>"
                        + "<p>JavaScript is disabled. Please click the button below to continue.</p>"
                        + "<button type='submit' class='btn'>Continue to Login</button>"
                        + "</noscript>"
                        + "</form>"
                        + "</div>"
                        + "</body></html>",
                redirectUrl, ticketId);
    }

    private void validateConfig(ViispIdentityProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("VIISP configuration cannot be null");
        }
        if (config.getServiceId() == null || config.getServiceId().trim().isEmpty()) {
            throw new IllegalArgumentException("VIISP Service ID must be configured");
        }
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(
            KeycloakSession session,
            UserSessionModel userSession,
            UriInfo uriInfo,
            RealmModel realm) {
        // VIISP doesn't support remote logout
        return null;
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {
        // No metadata export for VIISP
        return null;
    }

    @Override
    public void backchannelLogout(
            KeycloakSession session,
            UserSessionModel userSession,
            UriInfo uriInfo,
            RealmModel realm) {
        // VIISP doesn't support backchannel logout
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
        // VIISP doesn't support token retrieval
        return null;
    }

    // VIISP Callback Endpoint
    protected static class ViispEndpoint {

        private final ViispIdentityProvider provider;
        private final AuthenticationCallback callback;
        private final RealmModel realm;
        private final EventBuilder event;
        private final ViispXMLClient xmlClient;

        public ViispEndpoint(
                ViispIdentityProvider provider,
                AuthenticationCallback callback,
                RealmModel realm,
                EventBuilder event) {
            this.provider = provider;
            this.callback = callback;
            this.realm = realm;
            this.event = event;
            this.xmlClient = provider.xmlClient;
        }

        @POST
        @Path("/")
        public Response handleCallback(
                @FormParam("ticket") String ticket,
                @FormParam("customData") String customData,
                @Context HttpHeaders headers) {
            LOG.info("=== VIISP Callback Started ===");
            LOG.info("Ticket received: {}", ticket);
            LOG.info("CustomData received: {}", customData);
            LOG.info("Current timestamp: {}", System.currentTimeMillis() / 1000);

            // Log all available cookies
            if (headers != null) {
                LOG.info("Headers present, checking cookies...");
                headers.getCookies()
                        .forEach(
                                (name, cookie) -> {
                                    LOG.info("Cookie: {} = {}", name, cookie.getValue());
                                });
            }
            if (ticket == null || ticket.isEmpty()) {
                return callback.error(
                        provider.getConfig(), "No authentication ticket received from VIISP");
            }

            try {
                LOG.info("Attempting to restore auth session with customData: {}", customData);
                LOG.info(
                        "About to call callback.getAndVerifyAuthenticationSession() at timestamp: {}",
                        System.currentTimeMillis());

                // Restore auth session
                AuthenticationSessionModel authSession;
                try {
                    authSession = callback.getAndVerifyAuthenticationSession(customData);
                    LOG.info(
                            "callback.getAndVerifyAuthenticationSession() completed at timestamp: {}",
                            System.currentTimeMillis());
                } catch (Exception e) {
                    LOG.error(
                            "callback.getAndVerifyAuthenticationSession() threw exception: {}",
                            e.getMessage(),
                            e);
                    throw e;
                }

                if (authSession == null) {
                    LOG.error("Authentication session is NULL for customData: {}", customData);
                    LOG.error("This means the session was not found or verification failed");
                    LOG.error(
                            "Possible reasons: session timeout, wrong state/tab ID, or session was already consumed");
                    LOG.error(
                            "Realm SSO session max lifespan: {} seconds",
                            realm.getSsoSessionMaxLifespan());
                    LOG.error(
                            "Realm SSO session idle timeout: {} seconds",
                            realm.getSsoSessionIdleTimeout());
                    event.error("AUTHENTICATION_SESSION_NOT_FOUND");
                    return callback.error(
                            provider.getConfig(),
                            "Authentication session expired or invalid. Please try logging in again.");
                }

                LOG.info("Authentication session restored successfully: {}", authSession);
                LOG.info("Auth session ID: {}", authSession.getParentSession().getId());
                LOG.info(
                        "Auth session client: {}",
                        authSession.getClient() != null
                                ? authSession.getClient().getClientId()
                                : "null");
                LOG.info(
                        "Auth session parent session timestamp: {}",
                        authSession.getParentSession().getTimestamp());
                LOG.info(
                        "Time elapsed since creation: {} seconds",
                        (System.currentTimeMillis() / 1000)
                                - authSession.getParentSession().getTimestamp());
                LOG.info(
                        "Auth note VIISP_TICKET_ID: {}",
                        authSession.getAuthNote("VIISP_TICKET_ID"));
                LOG.info("Auth note VIISP_STATE: {}", authSession.getAuthNote("VIISP_STATE"));
                LOG.info(
                        "Auth note VIISP_STATE_COPY: {}",
                        authSession.getAuthNote("VIISP_STATE_COPY"));
                LOG.info(
                        "Auth note VIISP_TIMESTAMP: {}",
                        authSession.getAuthNote("VIISP_TIMESTAMP"));
                if (authSession.getAuthNote("VIISP_TIMESTAMP") != null) {
                    long storedTime = Long.parseLong(authSession.getAuthNote("VIISP_TIMESTAMP"));
                    long elapsedMs = System.currentTimeMillis() - storedTime;
                    LOG.info(
                            "Time since VIISP redirect: {} ms ({} seconds)",
                            elapsedMs,
                            elapsedMs / 1000);
                }

                // Retrieve user identity from VIISP using ticket
                ViispIdentityProviderConfig viispConfig =
                        new ViispIdentityProviderConfig(provider.getConfig());
                ViispUserInfo userInfo =
                        xmlClient.getUserInfo(ticket, viispConfig.getAuthServiceUrl());

                LOG.info("User info: {}", userInfo); // TODO: remove after testing

                // Create brokered identity context
                BrokeredIdentityContext identity = createBrokeredIdentity(userInfo);
                identity.setAuthenticationSession(authSession);

                LOG.info("Brokered identity context: {}", identity);
                LOG.info("Brokered identity attributes: {}", identity.getAttributes());
                return callback.authenticated(identity);
            } catch (Exception e) {
                event.error("VIISP_AUTHENTICATION_FAILED");
                return callback.error(
                        provider.getConfig(),
                        "Failed to process VIISP authentication: " + e.getMessage());
            }
        }

        private BrokeredIdentityContext createBrokeredIdentity(ViispUserInfo userInfo) {
            LOG.info("Creating Brokered Identity Context");
            BrokeredIdentityContext identity =
                    new BrokeredIdentityContext(userInfo.getPersonalCode(), provider.getConfig());

            // Set basic identity information
            LOG.info("Setting up Brokered Identity Context");
            identity.setUsername(userInfo.getPersonalCode());
            identity.setEmail(userInfo.getEmail());
            identity.setFirstName(userInfo.getFirstName());
            identity.setLastName(userInfo.getLastName());
            identity.setBrokerUserId(userInfo.getPersonalCode());

            // Add VIISP-specific custom attributes
            LOG.info("Settings up additional attributes");
            String personalUniqueId =
                    String.format(
                            "urn:schac:personalUniqueID:lt:nationalIDCard:%s",
                            userInfo.getPersonalCode());
            identity.setUserAttribute("schacPersonalUniqueID", personalUniqueId);
            identity.setUserAttribute("lt-personal-code", userInfo.getPersonalCode());
            identity.setUserAttribute("authentication-provider", userInfo.getAuthProvider());
            identity.setUserAttribute("companyName", userInfo.getCompanyName());
            identity.setUserAttribute("companyCode", userInfo.getCompanyCode());

            // Store authentication context
            LOG.info("Settings up provider");
            identity.setIdp(provider);

            return identity;
        }
    }
}
