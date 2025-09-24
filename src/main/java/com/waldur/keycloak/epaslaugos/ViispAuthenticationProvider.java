package com.waldur.keycloak.epaslaugos;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ViispAuthenticationProvider {

    AUTH_LOGIN_PASS("auth.login.pass"),
    AUTH_LT_IDENTITY_CARD("auth.lt.identity.card"),
    AUTH_LT_GOVERNMENT_EMPLOYEE_CARD("auth.lt.government.employee.card"),
    AUTH_LT_BANK("auth.lt.bank"),
    AUTH_EIDAS("auth.eidas"),
    AUTH_SIGNATURE_PROVIDER("auth.signatureProvider"),
    AUTH_ILTU_IDENTITY_CARD("auth.iltu.identity.card");
    
    private final String value;

    ViispAuthenticationProvider(String v) {
        value = v;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static ViispAuthenticationProvider fromValue(String v) {
        for (ViispAuthenticationProvider c: ViispAuthenticationProvider.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}