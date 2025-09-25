package com.waldur.keycloak.epaslaugos;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;

@JacksonXmlRootElement(localName = "authenticationRequest", namespace = "http://www.epaslaugos.lt/services/authentication")
public class ViispAuthenticationRequest {

	@JacksonXmlProperty(isAttribute = true, localName = "id")
	protected String id;

	@JacksonXmlProperty(localName = "pid", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected String pid;

	@JacksonXmlProperty(localName = "serviceTarget", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected ViispServiceTarget serviceTarget;

	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "authenticationProvider", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected List<ViispAuthenticationProvider> authenticationProvider;

	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "authenticationAttribute", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected List<ViispAuthenticationAttribute> authenticationAttribute;

	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "userInformation", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected List<ViispUserInformation> userInformation;

	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "proxyAuthenticationAttribute", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected List<ViispAuthenticationAttribute> proxyAuthenticationAttribute;

	@JacksonXmlElementWrapper(useWrapping = false)
	@JacksonXmlProperty(localName = "proxyUserInformation", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected List<ViispUserInformation> proxyUserInformation;

	@JacksonXmlProperty(localName = "postbackUrl", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected String postbackUrl;

	@JacksonXmlProperty(localName = "customData", namespace = "http://www.epaslaugos.lt/services/authentication")
	protected String customData;

	public String getId() {
		return id;
	}

	public void setId(String value) {
		this.id = value;
	}

	public String getPid() {
		return pid;
	}

	public void setPid(String value) {
		this.pid = value;
	}

	public String getPostbackUrl() {
		return postbackUrl;
	}

	public void setPostbackUrl(String value) {
		this.postbackUrl = value;
	}

	public String getCustomData() {
		return customData;
	}

	public void setCustomData(String value) {
		this.customData = value;
	}

	public List<ViispAuthenticationAttribute> getAuthenticationAttribute() {
		if (authenticationAttribute == null) {
			authenticationAttribute = new ArrayList<ViispAuthenticationAttribute>();
		}
		return this.authenticationAttribute;
	}

	public List<ViispAuthenticationProvider> getAuthenticationProvider() {
		if (authenticationProvider == null) {
			authenticationProvider = new ArrayList<ViispAuthenticationProvider>();
		}
		return this.authenticationProvider;
	}

	public ViispServiceTarget getServiceTarget() {
		return serviceTarget;
	}

	public void setServiceTarget(ViispServiceTarget value) {
		this.serviceTarget = value;
	}

	public List<ViispUserInformation> getUserInformation() {
		if (userInformation == null) {
			userInformation = new ArrayList<ViispUserInformation>();
		}
		return this.userInformation;
	}

	public List<ViispAuthenticationAttribute> getProxyAuthenticationAttribute() {
		if (proxyAuthenticationAttribute == null) {
			proxyAuthenticationAttribute = new ArrayList<ViispAuthenticationAttribute>();
		}
		return this.proxyAuthenticationAttribute;
	}

	public List<ViispUserInformation> getProxyUserInformation() {
		if (proxyUserInformation == null) {
			proxyUserInformation = new ArrayList<ViispUserInformation>();
		}
		return this.proxyUserInformation;
	}
}
