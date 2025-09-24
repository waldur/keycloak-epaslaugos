package com.waldur.keycloak.epaslaugos;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "authenticationDataRequest", namespace = "http://www.epaslaugos.lt/services/authentication")
public class ViispAuthenticationDataRequest {

    @JacksonXmlProperty(isAttribute = true, localName = "id")
    protected String id;
    
    @JacksonXmlProperty(localName = "pid", namespace = "http://www.epaslaugos.lt/services/authentication")
    protected String pid;
    
    @JacksonXmlProperty(localName = "ticket", namespace = "http://www.epaslaugos.lt/services/authentication")
    protected String ticket;
    
    @JacksonXmlProperty(localName = "includeSourceData", namespace = "http://www.epaslaugos.lt/services/authentication")
    protected Boolean includeSourceData;

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

    public String getTicket() {
        return ticket;
    }

    public void setTicket(String value) {
        this.ticket = value;
    }

    public Boolean isIncludeSourceData() {
        return includeSourceData;
    }

    public void setIncludeSourceData(Boolean value) {
        this.includeSourceData = value;
    }
}