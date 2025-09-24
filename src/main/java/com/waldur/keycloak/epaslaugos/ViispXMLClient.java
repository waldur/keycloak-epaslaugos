package com.waldur.keycloak.epaslaugos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.KeyValue;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.ExcC14NParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ViispXMLClient {

    public static final String SIGNED_NODE_ID = "uniqueNodeId";
    public static final String TEST_PID = "VSID000000000113";

    private static final XMLSignatureFactory XML_SIGNATURE_FACTORY = XMLSignatureFactory.getInstance("DOM");
    private static final Logger LOG = LoggerFactory.getLogger(ViispXMLClient.class);

    private final ViispIdentityProviderConfig config;
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;

    public ViispXMLClient(ViispIdentityProviderConfig config) {
        this.config = config;
        try {
            loadKeyStore();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize VIISP XML client", e);
        }
    }

    private void loadKeyStore() throws Exception {
        String keystorePath = config.getKeystorePath();
        String keystorePassword = config.getKeystorePassword();

        // Use test keystore if no custom path provided
        if (keystorePath == null || keystorePath.isEmpty()) {
            keystorePath = "/keystore-test.jks";
            keystorePassword = "viisp-test";
        }

        KeyStore keyStore = KeyStore.getInstance("JKS");

        if (keystorePath.startsWith("/")) {
            // Load from classpath
            keyStore.load(getClass().getResourceAsStream(keystorePath), keystorePassword.toCharArray());
        } else {
            // Load from file system
            keyStore.load(FileUtils.openInputStream(FileUtils.getFile(keystorePath)), keystorePassword.toCharArray());
        }

        for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements(); ) {
            String alias = e.nextElement();
            if (keyStore.isKeyEntry(alias)) {
                privateKey = (PrivateKey) keyStore.getKey(alias, keystorePassword.toCharArray());
                X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
                publicKey = cert.getPublicKey();
                break;
            }
        }

        if (privateKey == null || publicKey == null) {
            throw new RuntimeException("No valid key pair found in keystore");
        }
    }

    public String sendAuthRequest(String authRequest, boolean isTestMode) throws IOException, InterruptedException {
        String authRequestBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                authRequest +
                "</soap:Body>" +
                "</soap:Envelope>";
        String authServiceURL = isTestMode ?
                "https://test.epaslaugos.lt/services/services/auth" :
                "https://epaslaugos.lt/services/services/auth";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(authServiceURL))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(authRequestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        LOG.info("Auth response status: {}", response.statusCode());
        return response.body();
    }

    public HttpResponse<String> submitAuthenticationTicket(String ticketId, boolean isTestMode) throws IOException, InterruptedException {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket ID cannot be null or empty");
        }

        String viispAuthUrl = isTestMode
                ? "https://test.epaslaugos.lt/portal/external/services/authentication/v2"
                : "https://epaslaugos.lt/portal/external/services/authentication/v2";

        // Third-party provided format: form with dynamic action URL and ticket value
        String soapBodyFormat = "<form name=\"REQUEST\" method=\"post\" action=\"%s\"><input type=\"hidden\" name=\"ticket\" value=\"%s\"/></form>\n";
        String soapBody = String.format(soapBodyFormat, viispAuthUrl, ticketId.trim());

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(viispAuthUrl))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(soapBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
            throw new RuntimeException(String.format(
                    "VIISP ticket submission failed. Expected 302 redirect, got %d. Response: %s",
                    response.statusCode(), response.body()
            ));
        }

        return response;
    }

    private String buildAuthRequest(String serviceId, String callbackUrl) throws Exception {
        ViispAuthenticationRequest request = new ViispAuthenticationRequest();
        request.setId(SIGNED_NODE_ID);
        request.setPid(serviceId != null ? serviceId : TEST_PID);
//        request.setPostbackUrl(callbackUrl);
//        request.setCustomData(UUID.randomUUID().toString());

        // Set service target
        request.setServiceTarget(ViispServiceTarget.CITIZEN);

        // Add authentication providers
        request.getAuthenticationProvider().add(ViispAuthenticationProvider.AUTH_LOGIN_PASS);
        request.getAuthenticationProvider().add(ViispAuthenticationProvider.AUTH_LT_IDENTITY_CARD);
        request.getAuthenticationProvider().add(ViispAuthenticationProvider.AUTH_LT_GOVERNMENT_EMPLOYEE_CARD);
        request.getAuthenticationProvider().add(ViispAuthenticationProvider.AUTH_LT_BANK);
        request.getAuthenticationProvider().add(ViispAuthenticationProvider.AUTH_EIDAS);
        request.getAuthenticationProvider().add(ViispAuthenticationProvider.AUTH_SIGNATURE_PROVIDER);
        request.getAuthenticationProvider().add(ViispAuthenticationProvider.AUTH_ILTU_IDENTITY_CARD);

        // Add authentication attributes
        request.getAuthenticationAttribute().add(ViispAuthenticationAttribute.LT_PERSONAL_CODE);
        request.getAuthenticationAttribute().add(ViispAuthenticationAttribute.LT_COMPANY_CODE);
        request.getAuthenticationAttribute().add(ViispAuthenticationAttribute.LT_GOVERNMENT_EMPLOYEE_CODE);
        request.getAuthenticationAttribute().add(ViispAuthenticationAttribute.EIDAS_EID);
        request.getAuthenticationAttribute().add(ViispAuthenticationAttribute.LOGIN);
        request.getAuthenticationAttribute().add(ViispAuthenticationAttribute.ILTU_PERSONAL_CODE);

        // Add user information requests
        request.getUserInformation().add(ViispUserInformation.ID);
        request.getUserInformation().add(ViispUserInformation.FIRST_NAME);
        request.getUserInformation().add(ViispUserInformation.LAST_NAME);
        request.getUserInformation().add(ViispUserInformation.COMPANY_NAME);
        request.getUserInformation().add(ViispUserInformation.ADDRESS);
        request.getUserInformation().add(ViispUserInformation.EMAIL);
        request.getUserInformation().add(ViispUserInformation.PHONE_NUMBER);
        request.getUserInformation().add(ViispUserInformation.BIRTHDAY);
        request.getUserInformation().add(ViispUserInformation.COMPANY_NAME);
        request.getUserInformation().add(ViispUserInformation.NATIONALITY);
        request.getUserInformation().add(ViispUserInformation.PROXY_TYPE);
        request.getUserInformation().add(ViispUserInformation.PROXY_SOURCE);

        // Add proxy authentication attributes
        request.getProxyAuthenticationAttribute().add(ViispAuthenticationAttribute.LT_PERSONAL_CODE);
        request.getProxyAuthenticationAttribute().add(ViispAuthenticationAttribute.LT_COMPANY_CODE);
        request.getProxyAuthenticationAttribute().add(ViispAuthenticationAttribute.LT_GOVERNMENT_EMPLOYEE_CODE);
        request.getProxyAuthenticationAttribute().add(ViispAuthenticationAttribute.EIDAS_EID);
        request.getProxyAuthenticationAttribute().add(ViispAuthenticationAttribute.LOGIN);
        request.getProxyAuthenticationAttribute().add(ViispAuthenticationAttribute.ILTU_PERSONAL_CODE);

        request.getProxyUserInformation().add(ViispUserInformation.ID);
        request.getUserInformation().add(ViispUserInformation.FIRST_NAME);
        request.getUserInformation().add(ViispUserInformation.LAST_NAME);
        request.getUserInformation().add(ViispUserInformation.ADDRESS);
        request.getUserInformation().add(ViispUserInformation.EMAIL);
        request.getUserInformation().add(ViispUserInformation.PHONE_NUMBER);
        request.getUserInformation().add(ViispUserInformation.BIRTHDAY);
        request.getUserInformation().add(ViispUserInformation.NATIONALITY);

        Document doc = (Document) marshal(request);
        setIdAttribute(doc.getChildNodes().item(0));
        return getSignedXml(doc.getFirstChild(), "#" + request.getId());
    }

    public String requestAuthenticationTicket(String callbackUrl, String serviceId, boolean isTestMode) throws Exception {
        String authRequest = buildAuthRequest(serviceId, callbackUrl);
        LOG.info(authRequest); // TODO: remove after testing
        String ticketData = sendAuthRequest(authRequest, isTestMode);
        return parseTicketFromXml(ticketData);
    }

    public ViispUserInfo getUserInfo(String ticket) throws Exception {
        ViispAuthenticationDataRequest dataRequest = new ViispAuthenticationDataRequest();
        dataRequest.setId(SIGNED_NODE_ID);
        dataRequest.setPid(config.getServiceId() != null ? config.getServiceId() : TEST_PID);
        dataRequest.setIncludeSourceData(true);
        dataRequest.setTicket(ticket);

        Document doc = (Document) marshal(dataRequest);
        setIdAttribute(doc.getChildNodes().item(0));

        String xml = getSignedXml(doc.getFirstChild(), "#" + dataRequest.getId());

        // Mock user info for now - in production would parse VIISP response
        ViispUserInfo userInfo = new ViispUserInfo();
        userInfo.setPersonalCode("38001010000");
        userInfo.setFirstName("Test");
        userInfo.setLastName("User");
        userInfo.setEmail("test@example.com");
        userInfo.setAuthProvider("auth.lt.bank");

        return userInfo;
    }

    private Node marshal(Object data) throws Exception {
        LOG.info("Marshalling the object of class {}", data.getClass().getName());

        XmlMapper xmlMapper = new XmlMapper();
        String xmlString = xmlMapper.writeValueAsString(data);

        // Parse the XML string back to DOM Document
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlString)));

        // Add namespace declarations to the root element
        Element rootElement = document.getDocumentElement();
        rootElement.setAttribute("xmlns:ns2", "http://www.w3.org/2000/09/xmldsig#");
        rootElement.setAttribute("xmlns:ns3", "http://www.w3.org/2001/10/xml-exc-c14n#");

        return document;
    }

    private static String parseTicketFromXml(String xmlContent) throws
            ParserConfigurationException,
            IOException,
            SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        LOG.info("Ticket data: {}", xmlContent); // TODO: remove after testing
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

        NodeList ticketNodes = document.getElementsByTagNameNS(
                "http://www.epaslaugos.lt/services/authentication",
                "ticket"
        );
        if (ticketNodes.getLength() > 0) {
            return ticketNodes.item(0).getTextContent().trim();
        }

        return null;
    }

    private String getSignedXml(Node node, String referenceUri) throws Exception {
        signNode(node, referenceUri);

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer trans = tf.newTransformer();

        // Omit the XML declaration
        trans.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        trans.transform(new DOMSource(node), new StreamResult(output));

        return new String(output.toByteArray(), Charset.forName("UTF-8"));
    }

    private void signNode(Node node, String uri) throws Exception {
        DOMSignContext dsc = new DOMSignContext(privateKey, node);
        XMLSignatureFactory fac = XML_SIGNATURE_FACTORY;

        List<String> prefixList = new ArrayList<>();
        prefixList.add(node.getPrefix());
        C14NMethodParameterSpec spec = new ExcC14NParameterSpec(prefixList);
        List<Transform> transforms = new ArrayList<>();
        transforms.add(fac.newTransform(CanonicalizationMethod.ENVELOPED, (TransformParameterSpec) null));
        transforms.add(fac.newTransform(CanonicalizationMethod.EXCLUSIVE, spec));

        Reference ref = fac.newReference(uri, fac.newDigestMethod(
                        DigestMethod.SHA1, null),
                transforms,
                null,
                null
        );
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, spec),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
                Collections.singletonList(ref)
        );

        KeyInfoFactory kif = fac.getKeyInfoFactory();
        KeyValue kv = kif.newKeyValue(publicKey);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(kv));

        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);
    }

    private void setIdAttribute(Node node) {
        Node idAttribute = node.getAttributes().getNamedItem("id");
        if (idAttribute != null) {
            ((Element) node).setIdAttribute("id", true);
        }
    }
}