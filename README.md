# Keycloak VIISP Identity Provider

A Keycloak extension that integrates with Lithuania's VIISP (Valstybės informacinių išteklių sąveikumo platforma) government authentication platform.

## Overview

This extension provides a custom Identity Provider for Keycloak that enables authentication through Lithuania's eGovernment authentication services. Users can log in using their government-issued digital identities including electronic ID cards, mobile signatures, and internet banking credentials.

## Features

- **Government Authentication**: Integration with VIISP test and production environments
- **Digital Signatures**: XML request signing using RSA-SHA1 with exclusive canonicalization
- **Flexible Configuration**: Support for custom service IDs, keystores, and requested attributes
- **User Attribute Mapping**: Automatic mapping of Lithuanian personal codes, company codes, and personal information
- **Test Environment Support**: Built-in support for VIISP test environment with test certificates

## Implementation Details

### Architecture

The implementation follows Keycloak's Identity Provider SPI (Service Provider Interface) pattern:

- **`ViispIdentityProvider`**: Main provider class handling authentication flow
- **`ViispIdentityProviderFactory`**: Factory for creating provider instances
- **`ViispIdentityProviderConfig`**: Configuration model for VIISP-specific settings
- **`ViispXMLClient`**: Handles XML generation, signing, and VIISP communication
- **XML Model Classes**: JAX-B annotated classes for VIISP XML schema compliance

### Authentication Flow

```mermaid
sequenceDiagram
    participant User
    participant Keycloak
    participant VIISP as VIISP Platform
    participant Gov as Gov Services

    User->>Keycloak: 1. Initiate login
    Keycloak->>Keycloak: 2. Generate callback URL
    Keycloak->>VIISP: 3. Request authentication ticket<br/>(signed XML)
    VIISP-->>Keycloak: 4. Return ticket
    Keycloak->>User: 5. Redirect to VIISP portal<br/>with ticket
    User->>VIISP: 6. Access VIISP portal
    VIISP->>Gov: 7. User selects auth method<br/>(eID, mobile, bank)
    Gov-->>VIISP: 8. Authentication result
    VIISP->>Keycloak: 9. POST callback with<br/>authentication ticket
    Keycloak->>VIISP: 10. Retrieve user info<br/>(signed XML request)
    VIISP-->>Keycloak: 11. Return user data<br/>(personal code, name, etc.)
    Keycloak->>Keycloak: 12. Create/update user<br/>session
    Keycloak->>User: 13. Complete login
```

### Key Components

1. **XML Request Generation**: Creates signed authentication requests using VIISP XML schema
2. **Digital Signature**: Implements XML digital signatures with RSA-SHA1 and ExcC14N canonicalization
3. **Certificate Management**: Handles X.509 certificates and private keys from Java KeyStore
4. **User Attribute Mapping**: Maps VIISP user data to Keycloak user attributes

### Supported Authentication Methods

Through VIISP, users can authenticate using:
- **Electronic ID cards** (eID)
- **Mobile signatures** (Smart-ID, Mobile ID)
- **Internet banking** credentials
- **EU eIDAS** compatible identities

## Configuration

### Required Settings

- **Service ID**: VIISP-assigned service identifier
- **Keystore Path**: Path to Java KeyStore containing certificates
- **Keystore Password**: Password for the keystore
- **Test Mode**: Enable/disable test environment

### Optional Settings

- **Requested Attributes**: Comma-separated list of user attributes to request
- **Custom Callback URL**: Override default callback endpoint

## Build and Deployment

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- Keycloak 22.0+ (for deployment)

### Building

```bash
# Set JAVA_HOME to Homebrew OpenJDK
export JAVA_HOME=/opt/homebrew/opt/openjdk

# Build the extension
mvn clean package
```

### Development

```bash
# Install git pre-push hook for the spotless linter and formatter
mvn spotless:install-git-pre-push-hook
```

### Deployment

1. Copy the built JAR to Keycloak providers directory:
   ```bash
   cp target/waldur-keycloak-epaslaugos-1.0.0-jar-with-dependencies.jar $KEYCLOAK_HOME/providers/
   ```

2. Restart Keycloak or run build command:
   ```bash
   $KEYCLOAK_HOME/bin/kc.sh build
   ```

3. Configure the VIISP Identity Provider in Keycloak Admin Console

## Testing

The project includes comprehensive test utilities:

- **`TestViispAccess.java`**: Validates connectivity to VIISP test environment
- **`SimpleTestRunner.java`**: Tests core XML client functionality
- **Test certificates**: Included for development and testing

Run tests with:
```bash
JAVA_HOME=/opt/homebrew/opt/openjdk /opt/homebrew/opt/openjdk/bin/java -cp ".:src/main/resources" TestViispAccess
```

## License

MIT License - see [LICENSE](LICENSE) file for details.
