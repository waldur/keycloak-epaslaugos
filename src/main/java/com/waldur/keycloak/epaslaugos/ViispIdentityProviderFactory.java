package com.waldur.keycloak.epaslaugos;

import java.util.ArrayList;
import java.util.List;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;

public class ViispIdentityProviderFactory
        extends AbstractIdentityProviderFactory<ViispIdentityProvider> {

    public static final String PROVIDER_ID = "VIISP";
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty serviceIdProperty;
        serviceIdProperty = new ProviderConfigProperty();
        serviceIdProperty.setName("serviceId");
        serviceIdProperty.setLabel("VIISP Service ID");
        serviceIdProperty.setType(ProviderConfigProperty.STRING_TYPE);
        serviceIdProperty.setHelpText("ID of the VIISP service.");
        serviceIdProperty.setRequired(true);
        configProperties.add(serviceIdProperty);

        ProviderConfigProperty authServiceUrl = new ProviderConfigProperty();
        authServiceUrl.setName("authServiceUrl");
        authServiceUrl.setLabel("Auth service URL");
        authServiceUrl.setType(ProviderConfigProperty.URL_TYPE);
        authServiceUrl.setHelpText("URL of the auth service, for example https://test.epaslaugos.lt/services/services/auth");
        authServiceUrl.setRequired(true);
        authServiceUrl.setDefaultValue("https://test.epaslaugos.lt/services/services/auth");
        configProperties.add(authServiceUrl);

        ProviderConfigProperty keystorePathProperty = new ProviderConfigProperty();
        keystorePathProperty.setName("keystorePath");
        keystorePathProperty.setLabel("Keystore Path");
        keystorePathProperty.setType(ProviderConfigProperty.STRING_TYPE);
        keystorePathProperty.setHelpText(
                "Path to the keystore file. If starts with / the file is loaded from classpath, otherwise - from the filesystem.");
        keystorePathProperty.setRequired(true);
        keystorePathProperty.setDefaultValue("/keystore-test.jks");
        configProperties.add(keystorePathProperty);

        ProviderConfigProperty keystorePasswordProperty = new ProviderConfigProperty();
        keystorePasswordProperty.setName("keystorePassword");
        keystorePasswordProperty.setLabel("Keystore Password");
        keystorePasswordProperty.setType(ProviderConfigProperty.PASSWORD);
        keystorePasswordProperty.setHelpText("Password for the keystore.");
        keystorePasswordProperty.setRequired(true);
        keystorePasswordProperty.setDefaultValue("viisp-test");
        configProperties.add(keystorePasswordProperty);
    }

    @Override
    public String getName() {
        return "Lithuanian eGovernment (VIISP)";
    }

    @Override
    public ViispIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new ViispIdentityProvider(session, new ViispIdentityProviderConfig(model));
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new ViispIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }
}
