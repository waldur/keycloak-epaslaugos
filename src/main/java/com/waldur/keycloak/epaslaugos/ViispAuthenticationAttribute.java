package com.waldur.keycloak.epaslaugos;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ViispAuthenticationAttribute {

    LT_PERSONAL_CODE("lt-personal-code"),
    LT_COMPANY_CODE("lt-company-code"),
    LT_GOVERNMENT_EMPLOYEE_CODE("lt-government-employee-code"),
    EIDAS_EID("eidas-eid"),
    LOGIN("login"),
    ILTU_PERSONAL_CODE("iltu-personal-code");
    
    private final String value;

    ViispAuthenticationAttribute(String v) {
        value = v;
    }

    @JsonValue
    public String value() {
        return value;
    }

    public static ViispAuthenticationAttribute fromValue(String v) {
        for (ViispAuthenticationAttribute c: ViispAuthenticationAttribute.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }
}