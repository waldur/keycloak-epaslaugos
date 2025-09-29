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

        ProviderConfigProperty testModeProperty;
        testModeProperty = new ProviderConfigProperty();
        testModeProperty.setName("testMode");
        testModeProperty.setLabel("Test mode");
        testModeProperty.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        testModeProperty.setHelpText("Test mode flag.");
        testModeProperty.setRequired(true);
        testModeProperty.setDefaultValue("false");
        configProperties.add(testModeProperty);
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
