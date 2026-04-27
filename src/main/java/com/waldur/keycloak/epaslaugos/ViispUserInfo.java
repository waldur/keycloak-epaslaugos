package com.waldur.keycloak.epaslaugos;

public class ViispUserInfo {

    private String personalCode;
    private String firstName;
    private String lastName;
    private String email;
    private String companyCode;
    private String authProvider;
    private String birthday;
    private String companyName;
    private String address;
    private String phoneNumber;
    private String nationality;
    private String proxyType;
    private String proxySource;

    public ViispUserInfo() {}

    public String getPersonalCode() {
        return personalCode;
    }

    public void setPersonalCode(String personalCode) {
        this.personalCode = personalCode;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCompanyCode() {
        return companyCode;
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getProxyType() {
        return proxyType;
    }

    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public String getProxySource() {
        return proxySource;
    }

    public void setProxySource(String proxySource) {
        this.proxySource = proxySource;
    }

    @Override
    public String toString() {
        return "ViispUserInfo{"
                + "personalCode='"
                + personalCode
                + '\''
                + ", firstName='"
                + firstName
                + '\''
                + ", lastName='"
                + lastName
                + '\''
                + ", email='"
                + email
                + '\''
                + ", companyCode='"
                + companyCode
                + '\''
                + ", authProvider='"
                + authProvider
                + '\''
                + ", birthday='"
                + birthday
                + '\''
                + ", companyName='"
                + companyName
                + '\''
                + ", address='"
                + address
                + '\''
                + ", phoneNumber='"
                + phoneNumber
                + '\''
                + ", nationality='"
                + nationality
                + '\''
                + ", proxyType='"
                + proxyType
                + '\''
                + ", proxySource='"
                + proxySource
                + '\''
                + '}';
    }
}
