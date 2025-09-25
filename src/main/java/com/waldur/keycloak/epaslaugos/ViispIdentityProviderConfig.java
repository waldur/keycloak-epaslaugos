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

    public boolean isTestMode() {
        return getConfig() != null ? Boolean.parseBoolean(getConfig().get("testMode")) : true;
    }

    public void setTestMode(boolean testMode) {
        if (getConfig() != null) {
            getConfig().put("testMode", String.valueOf(testMode));
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
