package com.waldur.keycloak.epaslaugos;

import org.keycloak.models.IdentityProviderModel;

public class ViispIdentityProviderConfig extends IdentityProviderModel {

    public ViispIdentityProviderConfig() {
        super();
    }

    public ViispIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }

    public String getServiceId() {
        return getConfig() != null ? getConfig().get("serviceId") : null;
    }

    public void setServiceId(String serviceId) {
        if (getConfig() != null) {
            getConfig().put("serviceId", serviceId);
        }
    }

    public String getKeystorePath() {
        return getConfig() != null ? getConfig().get("keystorePath") : null;
    }

    public void setKeystorePath(String keystorePath) {
        if (getConfig() != null) {
            getConfig().put("keystorePath", keystorePath);
        }
    }

    public String getKeystorePassword() {
        return getConfig() != null ? getConfig().get("keystorePassword") : null;
    }

    public void setKeystorePassword(String keystorePassword) {
        if (getConfig() != null) {
            getConfig().put("keystorePassword", keystorePassword);
        }
    }

    public String getAuthServiceUrl() {
        return getConfig() != null
                ? getConfig().get("authServiceUrl")
                : "https://test.epaslaugos.lt/services/services/auth";
    }

    public void setAuthServiceUrl(String authServiceUrl) {
        if (getConfig() != null) {
            getConfig().put("authServiceUrl", authServiceUrl);
        }
    }

    public String getRedirectServiceUrl() {
        return getConfig() != null
                ? getConfig().get("redirectServiceUrl")
                : "https://test.epaslaugos.lt/portal/external/services/authentication/v2";
    }

    public void setRedirectServiceUrl(String redirectServiceUrl) {
        if (getConfig() != null) {
            getConfig().put("redirectServiceUrl", redirectServiceUrl);
        }
    }

    public String getRequestedAttributes() {
        return getConfig() != null
                ? getConfig()
                        .getOrDefault(
                                "requestedAttributes",
                                "lt-personal-code,lt-company-code,firstName,lastName,email")
                : "lt-personal-code,lt-company-code,firstName,lastName,email";
    }

    public void setRequestedAttributes(String attributes) {
        if (getConfig() != null) {
            getConfig().put("requestedAttributes", attributes);
        }
    }
}
