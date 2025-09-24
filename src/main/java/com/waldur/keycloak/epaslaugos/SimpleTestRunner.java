package com.waldur.keycloak.epaslaugos;

public class SimpleTestRunner {
    
    public static void main(String[] args) {
        try {
            System.out.println("Testing VIISP XML Client...");
            
            // Create test config
            ViispIdentityProviderConfig config = new ViispIdentityProviderConfig();
            
            // Test XML client initialization
            ViispXMLClient client = new ViispXMLClient(config);
            
            System.out.println("✓ ViispXMLClient created successfully");
            
            // Test ticket generation
            String ticket = client.requestAuthenticationTicket("http://test-callback", "VSID000000000113", true);
            System.out.println("✓ Generated authentication ticket: " + ticket);
            
            // Test user info retrieval
            ViispUserInfo userInfo = client.getUserInfo(ticket);
            System.out.println("✓ Retrieved user info:");
            System.out.println("  Personal Code: " + userInfo.getPersonalCode());
            System.out.println("  Name: " + userInfo.getFirstName() + " " + userInfo.getLastName());
            System.out.println("  Email: " + userInfo.getEmail());
            
            System.out.println("\n✓ All tests passed!");
            
        } catch (Exception e) {
            System.err.println("✗ Test failed:");
            e.printStackTrace();
        }
    }
}