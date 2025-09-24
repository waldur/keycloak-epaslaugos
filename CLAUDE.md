# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Keycloak Identity Provider extension for VIISP (Valstybės informacinių išteklių sąveikumo platforma) - Lithuania's government authentication platform. The project implements a custom Identity Provider that integrates Keycloak with VIISP's proprietary XML/SOAP authentication protocol.

## Build and Development Commands

### Java Requirements
- **OpenJDK 17+** required (project uses Java 17 target)
- **Homebrew OpenJDK**: Use `/opt/homebrew/opt/openjdk/bin/java` and `/opt/homebrew/opt/openjdk/bin/javac`
- **Set JAVA_HOME**: `export JAVA_HOME=/opt/homebrew/opt/openjdk`

### Maven Commands
- **Build the project**: `JAVA_HOME=/opt/homebrew/opt/openjdk mvn clean package`
- **Run tests**: `JAVA_HOME=/opt/homebrew/opt/openjdk mvn test`
- **Install to local repository**: `JAVA_HOME=/opt/homebrew/opt/openjdk mvn install`

### Manual Compilation (if Maven unavailable)
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk
$JAVA_HOME/bin/javac -cp "lib/*" src/main/java/com/waldur/keycloak/epaslaugos/*.java
```

### Project Structure
- `src/main/java/com/waldur/keycloak/epaslaugos/` - Main source code
  - `ViispIdentityProvider.java` - Core identity provider implementation
  - `ViispIdentityProviderFactory.java` - Factory for creating provider instances
  - `ViispIdentityProviderConfig.java` - Configuration model
  - `ViispSOAPClient.java` - SOAP client for VIISP communication
  - `ViispUserInfo.java` - User information model
- `src/main/resources/META-INF/services/` - SPI registration files
- `pom.xml` - Maven configuration with Keycloak and SOAP dependencies

## Architecture

### VIISP Integration Architecture
The extension implements Keycloak's Identity Provider SPI to handle VIISP's custom XML-based authentication flow:

1. **Authentication Initiation**: User initiates login through Keycloak
2. **XML Ticket Request**: Extension creates signed XML authentication request for VIISP
3. **User Redirection**: User is redirected to VIISP portal for authentication
4. **Callback Handling**: VIISP posts authentication ticket back to Keycloak
5. **User Data Retrieval**: Extension retrieves user information using signed XML data request
6. **Identity Brokering**: User is logged into Keycloak with VIISP identity

### Key Components
- **AbstractIdentityProvider**: Extended to implement VIISP-specific authentication flow
- **XML Client**: Handles JAX-B marshalling and XML digital signatures
- **X.509 Certificates**: Used for XML digital signatures (JKS keystore format)
- **JAX-B Models**: Type-safe XML request/response models matching VIISP schema

## Configuration Requirements

### VIISP Configuration
- **Service ID**: VIISP-assigned service identifier (e.g., VSID000000000113 for testing)
- **Keystore Path**: Path to JKS keystore containing signing certificate
- **Keystore Password**: Password for the keystore
- **Test Mode**: Boolean flag for test vs production environment
- **Requested Attributes**: Comma-separated list of user attributes to request

### Certificate Management
Certificates must be in Java KeyStore (.jks) format:
```bash
# Convert PEM to JKS if needed
keytool -importkeystore -srckeystore cert.p12 -srcstoretype PKCS12 -destkeystore viisp.jks
```

## Deployment

### Keycloak Deployment
1. Build JAR: `mvn clean package`
2. Copy to Keycloak providers directory
3. Rebuild Keycloak: `$KEYCLOAK_HOME/bin/kc.sh build`
4. Configure provider in Keycloak Admin Console

### Dependencies
- Keycloak 26.3.4 (provided scope)
- JAX-B API 2.3.0 for XML binding
- JAX-B Implementation 2.3.0
- Commons IO 2.3 for file operations
- Commons Lang 2.6 for utilities

## Security Considerations

- All XML requests use RSA-SHA1 digital signatures with exclusive canonicalization
- Certificates stored in JKS keystore format with secure password protection
- Test keystore included in resources for development (`keystore-test.jks`)
- GDPR compliance required for Lithuanian government integration

## Testing

- **Test Service ID**: `VSID000000000113` (pre-configured)
- **Test Environment**: `https://www.epaslaugos.lt/portal-test/`
- **Test Keystore**: `keystore-test.jks` with password `viisp-test`
- Implementation includes mock responses for initial testing without VIISP connectivity