package com.waldur.keycloak.epaslaugos;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ViispUserInformation {
	ID("id"), FIRST_NAME("firstName"), LAST_NAME("lastName"), COMPANY_NAME("companyName"), ADDRESS("address"), EMAIL(
			"email"), PHONE_NUMBER("phoneNumber"), BIRTHDAY(
					"birthday"), NATIONALITY("nationality"), PROXY_TYPE("proxyType"), PROXY_SOURCE("proxySource");

	private final String value;

	ViispUserInformation(String v) {
		value = v;
	}

	@JsonValue
	public String value() {
		return value;
	}

	public static ViispUserInformation fromValue(String v) {
		for (ViispUserInformation c : ViispUserInformation.values()) {
			if (c.value.equals(v)) {
				return c;
			}
		}
		throw new IllegalArgumentException(v);
	}
}
