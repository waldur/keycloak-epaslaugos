package com.waldur.keycloak.epaslaugos;

import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.UUID;

public class ViispIdentityProvider extends AbstractIdentityProvider<IdentityProviderModel> {
    
    public static final String PROVIDER_ID = "viisp";
    
    private final ViispXMLClient xmlClient;

    private final Logger logger = LoggerFactory.getLogger(ViispIdentityProvider.class);

    public ViispIdentityProvider(KeycloakSession session, ViispIdentityProviderConfig config) {
        super(session, config);
        this.xmlClient = new ViispXMLClient(config);
    }
    
    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new ViispEndpoint(this, callback, realm, event);
    }
    
    @Override
    public Response performLogin(AuthenticationRequest request) {
        logger.info("Started login process using VIISP IDP");
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

            // Use the default redirect URL
            String callbackUrl = request.getRedirectUri();
            logger.info("Callback URL: {}", callbackUrl);

            // Initialize and validate VIISP configuration
            ViispIdentityProviderConfig viispConfig = new ViispIdentityProviderConfig(getConfig());
            logger.info("Viisp Identity Provider Config: {}", viispConfig.getConfig()); // TODO: remove after testing
            validateConfig(viispConfig);

            // Request authentication ticket from VIISP
            String ticketId = xmlClient.requestAuthenticationTicket(callbackUrl, viispConfig.getServiceId(), viispConfig.isTestMode());
            if (ticketId == null || ticketId.trim().isEmpty()) {
                throw new IdentityBrokerException("Failed to obtain authentication ticket from VIISP");
            }

            // Submit the ticket and get redirect response
            HttpResponse<String> ticketResponse = xmlClient.submitAuthenticationTicket(ticketId, viispConfig.isTestMode());

            // Build and return redirect response to VIISP
            return buildLoginResponse(ticketResponse);
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

    private void validateConfig(ViispIdentityProviderConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("VIISP configuration cannot be null");
        }
        if (config.getServiceId() == null || config.getServiceId().trim().isEmpty()) {
            throw new IllegalArgumentException("VIISP Service ID must be configured");
        }
    }

    private Response buildLoginResponse(HttpResponse<String> ticketResponse) {
        if (ticketResponse == null) {
            throw new IdentityBrokerException("Ticket response cannot be null");
        }

        // Extract location header from VIISP redirect response
        String locationUrl = ticketResponse.headers()
            .firstValue("Location")
            .orElseThrow(() -> new IdentityBrokerException("Missing Location header in VIISP ticket response"));

        // Build response with proper redirect
        Response.ResponseBuilder responseBuilder = Response.seeOther(UriBuilder.fromUri(locationUrl).build());

        // Preserve authentication cookies from VIISP
        var cookies = ticketResponse.headers().allValues("Set-Cookie");
        if (!cookies.isEmpty()) {
            // Each cookie needs its own Set-Cookie header (HTTP standard)
            for (String cookie : cookies) {
                responseBuilder.header("Set-Cookie", cookie);
            }
        }

        return responseBuilder.build();
    }
    
    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, 
            UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        // VIISP doesn't support remote logout
        return null;
    }
    
    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {
        // No metadata export for VIISP
        return null;
    }
    
    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, 
            UriInfo uriInfo, RealmModel realm) {
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
        
        public ViispEndpoint(ViispIdentityProvider provider, AuthenticationCallback callback,
                RealmModel realm, EventBuilder event) {
            this.provider = provider;
            this.callback = callback;
            this.realm = realm;
            this.event = event;
            this.xmlClient = provider.xmlClient;
        }
        
        @POST
        @Path("/")
        public Response handleCallback(@FormParam("ticket") String ticket,
                @Context HttpHeaders headers) {
            
            if (ticket == null || ticket.isEmpty()) {
                return callback.error(provider.getConfig(), "No authentication ticket received from VIISP");
            }
            
            try {
                // Retrieve user identity from VIISP using ticket
                ViispUserInfo userInfo = xmlClient.getUserInfo(ticket);
                
                // Create brokered identity context
                BrokeredIdentityContext identity = createBrokeredIdentity(userInfo);
                
                return callback.authenticated(identity);
                
            } catch (Exception e) {
                event.error("VIISP_AUTHENTICATION_FAILED");
                return callback.error(provider.getConfig(), "Failed to process VIISP authentication: " + e.getMessage());
            }
        }
        
        private BrokeredIdentityContext createBrokeredIdentity(ViispUserInfo userInfo) {
            BrokeredIdentityContext identity = new BrokeredIdentityContext(
                userInfo.getPersonalCode(), provider.getConfig()
            );
            
            // Set basic identity information
            identity.setUsername(userInfo.getPersonalCode());
            identity.setEmail(userInfo.getEmail());
            identity.setFirstName(userInfo.getFirstName());
            identity.setLastName(userInfo.getLastName());
            identity.setBrokerUserId(userInfo.getPersonalCode());
            
            // Add VIISP-specific custom attributes
            identity.setUserAttribute("lt-personal-code", userInfo.getPersonalCode());
            identity.setUserAttribute("lt-company-code", userInfo.getCompanyCode());
            identity.setUserAttribute("authentication-provider", userInfo.getAuthProvider());
            identity.setUserAttribute("birthday", userInfo.getBirthday());
            identity.setUserAttribute("company-name", userInfo.getCompanyName());
            
            // Store authentication context
            identity.setIdp(provider);
            
            return identity;
        }
    }
}
