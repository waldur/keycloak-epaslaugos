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
import org.keycloak.events.EventBuilder;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
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

			// Use the default redirect URL
			String callbackUrl = request.getRedirectUri();
			LOG.info("Callback URL: {}", callbackUrl);

			// Initialize and validate VIISP configuration
			ViispIdentityProviderConfig viispConfig = new ViispIdentityProviderConfig(getConfig());
			LOG.info("Viisp Identity Provider Config: {}", viispConfig.getConfig()); // TODO: remove after testing
			validateConfig(viispConfig);

			// Request authentication ticket from VIISP
			String ticketId = xmlClient.requestAuthenticationTicket(callbackUrl, viispConfig.getServiceId(),
					viispConfig.isTestMode());
			if (ticketId == null || ticketId.trim().isEmpty()) {
				throw new IdentityBrokerException("Failed to obtain authentication ticket from VIISP");
			}

			String ticketSubmitPage = createTicketSubmitPage(ticketId);
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

	private String createTicketSubmitPage(String ticketId) {
		return String.format("<!DOCTYPE html>" + "<html><head>" + "<meta charset='utf-8'>"
				+ "<meta name='viewport' content='width=device-width, initial-scale=1'>"
				+ "<title>Auth to VIISP</title>" + "<style>"
				+ "body { font-family: Arial, sans-serif; display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; background-color: #f5f5f5; }"
				+ ".container { text-align: center; background: white; padding: 2rem; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }"
				+ ".btn { background-color: #007bff; color: white; padding: 12px 24px; border: none; border-radius: 4px; cursor: pointer; font-size: 16px; }"
				+ ".btn:hover { background-color: #0056b3; }" + "</style>"
				+ "<script>window.onload=function(){document.forms['REQUEST'].submit();}</script>" + "</head>"
				+ "<body>" + "<div class='container'>" + "<h2>Redirecting to VIISP...</h2>"
				+ "<p>Please wait while we redirect you to the authentication service.</p>"
				+ "<form name='REQUEST' method='post' action='https://test.epaslaugos.lt/portal/external/services/authentication/v2'>"
				+ "<input type='hidden' name='ticketId' value='%s'/>" + "<noscript>"
				+ "<p>JavaScript is disabled. Please click the button below to continue.</p>"
				+ "<button type='submit' class='btn'>Continue to Login</button>" + "</noscript>" + "</form>" + "</div>"
				+ "</body></html>", ticketId);
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
	public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession,
			UriInfo uriInfo, RealmModel realm) {
		// VIISP doesn't support remote logout
		return null;
	}

	@Override
	public Response export(UriInfo uriInfo, RealmModel realm, String format) {
		// No metadata export for VIISP
		return null;
	}

	@Override
	public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo,
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

		public ViispEndpoint(ViispIdentityProvider provider, AuthenticationCallback callback, RealmModel realm,
				EventBuilder event) {
			this.provider = provider;
			this.callback = callback;
			this.realm = realm;
			this.event = event;
			this.xmlClient = provider.xmlClient;
		}

		@POST
		@Path("/")
		public Response handleCallback(@FormParam("ticket") String ticket, @Context HttpHeaders headers) {
			LOG.info("Ticket data after login: {}", ticket); // TODO: remove after testing
			if (ticket == null || ticket.isEmpty()) {
				return callback.error(provider.getConfig(), "No authentication ticket received from VIISP");
			}

			try {
				// Retrieve user identity from VIISP using ticket
				ViispIdentityProviderConfig viispConfig = new ViispIdentityProviderConfig(provider.getConfig());
				ViispUserInfo userInfo = xmlClient.getUserInfo(ticket, viispConfig.isTestMode());

				// Create brokered identity context
				BrokeredIdentityContext identity = createBrokeredIdentity(userInfo);

				return callback.authenticated(identity);

			} catch (Exception e) {
				event.error("VIISP_AUTHENTICATION_FAILED");
				return callback.error(provider.getConfig(),
						"Failed to process VIISP authentication: " + e.getMessage());
			}
		}

		private BrokeredIdentityContext createBrokeredIdentity(ViispUserInfo userInfo) {
			BrokeredIdentityContext identity = new BrokeredIdentityContext(userInfo.getPersonalCode(),
					provider.getConfig());

			// Set basic identity information
			identity.setUsername(userInfo.getPersonalCode());
			identity.setEmail(userInfo.getEmail());
			identity.setFirstName(userInfo.getFirstName());
			identity.setLastName(userInfo.getLastName());
			identity.setBrokerUserId(userInfo.getPersonalCode());

			// Add VIISP-specific custom attributes
			identity.setUserAttribute("lt-personal-code", userInfo.getPersonalCode());
			identity.setUserAttribute("authentication-provider", userInfo.getAuthProvider());

			// Store authentication context
			identity.setIdp(provider);

			return identity;
		}
	}
}
