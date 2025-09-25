package com.waldur.keycloak.epaslaugos;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ViispServiceTarget {
	CITIZEN("citizen"), BUSINESS("business"), PROVIDER("provider");

	private final String value;

	ViispServiceTarget(String v) {
		value = v;
	}

	@JsonValue
	public String value() {
		return value;
	}

	public static ViispServiceTarget fromValue(String v) {
		for (ViispServiceTarget c : ViispServiceTarget.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}
}
