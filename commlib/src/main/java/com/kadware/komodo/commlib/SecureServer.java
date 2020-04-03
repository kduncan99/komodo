/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.*;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.GregorianCalendar;

public class SecureServer {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class Configurator extends HttpsConfigurator {
        private Configurator(SSLContext context) { super(context); }

        @Override
        public void configure(
            HttpsParameters params
        ) {
            try {
                SSLContext sslContext = SSLContext.getDefault();
                SSLEngine sslEngine = sslContext.createSSLEngine();
                params.setNeedClientAuth(true);
                params.setCipherSuites(sslEngine.getEnabledCipherSuites());
                params.setProtocols(sslEngine.getEnabledProtocols());

                SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
                params.setSSLParameters(defaultSSLParameters);
            } catch (Exception e) {
                LOGGER.catching(e);
            }
        }
    }

    private static class CustomKeyManager extends X509ExtendedKeyManager {

        private final String _keyPassword;
        private final KeyStore _keyStore;

        /**
         * Constructor
         * @param keyStore the KeyStore containing the certificates to use as keys
         * @param keyPassword the password to use for any private entries
         */
        CustomKeyManager(
            final KeyStore keyStore,
            final String keyPassword
        ) {
            _keyStore = keyStore;
            _keyPassword = keyPassword;
        }

        /**
         * For HTTPS clients only - we're not a client, so we don't use this
         */
        @Override
        public String chooseClientAlias(
            final String[] keyType,
            final Principal[] issuers,
            final Socket socket
        ) {
            return null;
        }

        /**
         * For HTTPS clients only - we're not a client, so we don't use this
         */
        @Override
        public String chooseEngineClientAlias(
            final String[] keyType,
            final Principal[] issuers,
            final SSLEngine engine
        ) {
            return null;
        }

        /**
         * Return the preferred alias if it exists in the keyStore; otherwise return the default.
         */
        @Override
        public String chooseEngineServerAlias(
            final String keyType,
            final Principal[] issuers,
            final SSLEngine engine
        ) {
            return DEFAULT_ALIAS;
        }

        /**
         * Return the preferred alias if it exists in the keyStore; otherwise return the default.
         */
        @Override
        public String chooseServerAlias(
            final String keyType,
            final Principal[] issuers,
            final Socket socket
        ) {
            return DEFAULT_ALIAS;
        }

        /**
         * Returns certificate chain with the certificate indicated by the alias first, then by subsequent signing certificates
         * @param alias alias of certificate of interest
         * @return chain of certificates
         */
        @Override
        public X509Certificate[] getCertificateChain(
            final String alias
        ) {
            try {
                Certificate[] certs = _keyStore.getCertificateChain(alias);
                X509Certificate[] result = new X509Certificate[certs.length];
                for (int cx = 0; cx < certs.length; ++cx) {
                    result[cx] = (X509Certificate) certs[cx];
                }
                return result;
            } catch (KeyStoreException e) {
                LOGGER.catching(e);
            }

            return new X509Certificate[0];
        }

        /**
         * For HTTPS clients only - we're not a client, so we don't use this
         */
        @Override
        public String[] getClientAliases(
            final String keyType,
            final Principal[] issuers
        ) {
            return null;
        }

        /**
         * Retrieves the private key for the indicated alias
         * @param alias indicates the key-pair of interest
         */
        @Override
        public PrivateKey getPrivateKey(
            final String alias
        ) {
            try {
                KeyStore.ProtectionParameter protection = new KeyStore.PasswordProtection(_keyPassword.toCharArray());
                KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) _keyStore.getEntry(alias, protection);
                return pkEntry.getPrivateKey();
            } catch (KeyStoreException
                | NoSuchAlgorithmException
                | UnrecoverableEntryException e) {
                LOGGER.catching(e);
                return null;
            }
        }

        /**
         * Retrieves the set of aliases which can be used for an HTTPS server
         * @param keyType must be "RSA"
         * @param issuers ignored
         */
        @Override
        public String[] getServerAliases(
            final String keyType,
            final Principal[] issuers
        ) {
            try {
                ArrayList<String> list = new ArrayList<>();
                KeyStore.ProtectionParameter protection = new KeyStore.PasswordProtection(_keyPassword.toCharArray());
                while (_keyStore.aliases().hasMoreElements()) {
                    String alias = _keyStore.aliases().nextElement();
                    KeyStore.Entry entry = _keyStore.getEntry(alias, protection);
                    if (entry instanceof KeyStore.PrivateKeyEntry) {
                        list.add(alias);
                    }
                }
                return (String[]) list.toArray();
            } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
                return null;
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data items
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(SecureServer.class);

    private static final int CONNECTION_BACKLOG             = 32;
    private static final String KEY_ENTRY_PASSWORD          = "horsehammerballmachine";
    private static final String KEY_STORE_PASSWORD          = "snailrocketgagjupiter";
    private static final String KEY_STORE_TYPE              = "JKS";
    private static final String TRUST_MANAGER_TYPE          = "SunX509";

    private static final String SSL_TYPE                    = "TLS";
    private static final String KEY_TYPE                    = "RSA";
    private static final String ALGORITHM                   = "SHA256WithRSA";
    private static final int KEY_SIZE                       = 2048;
    private static final long VALIDITY                      = 3650 * 24 * 60 * 60;  //  3650 days, expressed in seconds

    private static final String DEFAULT_ALIAS               = "default";
    private static final String DEFAULT_CERT_X500NAME_OU    = "Komodo";         //  organizational unit
    private static final String DEFAULT_CERT_X500NAME_DC    = "KadWare";        //  domain component
    private static final String DEFAULT_CERT_X500NAME_L     = "Denver";         //  locality name
    private static final String DEFAULT_CERT_X500NAME_ST    = "Colorado";       //  state or province name
    private static final String DEFAULT_CERT_X500NAME_C     = "US";             //  country name

    private final String _commonName;
    private final int _portNumber;
    private HttpsServer _server;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param commonName properly formatted distinguished name string
     * @param portNumber port number upon which we listen
     */
    public SecureServer(
        final String commonName,
        final int portNumber
    ) {
        _commonName = commonName;
        _portNumber = portNumber;
    }

    /**
     * Appends a handler with an associated path to the created server.
     * Call after setup() and before start()
     */
    public void appendHandler(
        final String path,
        final HttpHandler handler
    ) {
        _server.createContext(path, handler);
    }

    public void setup(
    ) throws CertificateException,
             InvalidKeyException,
             IOException,
             KeyManagementException,
             KeyStoreException,
             NoSuchAlgorithmException,
             NoSuchProviderException,
             SignatureException {
        KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
        keyStore.load(null, KEY_STORE_PASSWORD.toCharArray());
        createSelfSignedCertificate(keyStore);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TRUST_MANAGER_TYPE);
        trustManagerFactory.init(keyStore);

        KeyManager[] newKeyManagers = new KeyManager[1];
        newKeyManagers[0] = new CustomKeyManager(keyStore, KEY_ENTRY_PASSWORD);

        SSLContext sslContext = SSLContext.getInstance(SSL_TYPE);
        sslContext.init(newKeyManagers, trustManagerFactory.getTrustManagers(), null);

        _server = HttpsServer.create();
        _server.setHttpsConfigurator(new Configurator(sslContext));

        InetAddress inetAddress = InetAddress.getByName("::");
        InetSocketAddress isAddr = new InetSocketAddress(inetAddress, _portNumber);
        _server.bind(isAddr, CONNECTION_BACKLOG);
    }

    public String getCommonName() {
        return _commonName;
    }

    public int getPortNumber() {
        return _portNumber;
    }

    /**
     * Starts the server
     */
    public void start() {
        _server.start();
    }

    /**
     * Stops the server
     */
    public void stop() {
        _server.stop(0);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    private void createSelfSignedCertificate(
        KeyStore keyStore
    ) throws CertificateException,
             InvalidKeyException,
             IOException,
             KeyStoreException,
             NoSuchAlgorithmException,
             NoSuchProviderException,
             SignatureException {
        if (keyStore.containsAlias(DEFAULT_ALIAS)) {
            keyStore.deleteEntry(DEFAULT_ALIAS);
        }

        //  Generate key pair
        CertAndKeyGen keyPair = new CertAndKeyGen(KEY_TYPE, ALGORITHM);
        keyPair.generate(KEY_SIZE);

        //  Generate certificate with appropriate extensions
        String distNameStr = createDistinguisedNameString(_commonName,
                                                          DEFAULT_CERT_X500NAME_OU,
                                                          DEFAULT_CERT_X500NAME_DC,
                                                          DEFAULT_CERT_X500NAME_L,
                                                          DEFAULT_CERT_X500NAME_ST,
                                                          DEFAULT_CERT_X500NAME_C);
        X500Name distinguishedName = new X500Name(distNameStr);

        //  Set up extensions
        CertificateExtensions extensions = new CertificateExtensions();

        //  Always include KeyUsage.
        //  Note that the KeyUsageExtension constructor does not provide a way to indicate criticality (which should be true),
        //  so once we've created the object, we have to re-create with the generic constructor to set that flag true.
        boolean[] flags = new boolean[9];
        flags[5] = true;    //  keyCertSign flag
        flags[6] = true;    //  Crl_Sign flag
        KeyUsageExtension kuExtension = new KeyUsageExtension(flags);
        Extension generalKUExtension = Extension.newExtension(kuExtension.getExtensionId(), true, kuExtension.getValue());
        extensions.set(KeyUsageExtension.NAME, generalKUExtension);

        //  Always include key identifier
        KeyIdentifier keyIdentifier = new KeyIdentifier(keyPair.getPublicKey());
        SubjectKeyIdentifierExtension skiExtension = new SubjectKeyIdentifierExtension(keyIdentifier.getIdentifier());
        extensions.set(SubjectKeyIdentifierExtension.NAME, skiExtension);

        //  Create the X509Certificate object
        X509Certificate[] certificateChain = new X509Certificate[1];
        certificateChain[0] = keyPair.getSelfCertificate(distinguishedName,
                                                         new GregorianCalendar().getTime(),
                                                         VALIDITY,
                                                         extensions);

        //  Store private key and the certificate chain (consisting of the single certificate) in the keyStore
        keyStore.setKeyEntry(DEFAULT_ALIAS, keyPair.getPrivateKey(), KEY_ENTRY_PASSWORD.toCharArray(), certificateChain);
    }

    /**
     * Creates a distinguished name string (DNS) given the separate components thereof
     */
    private static String createDistinguisedNameString(
        final String commonName,
        final String orgUnit,
        final String orgName,
        final String location,
        final String state,
        final String country
    ) {
        String distNameStr
            = ((commonName != null) ? "CN=" + commonName + "," : "")
              + ((orgUnit != null) ? "OU=" + orgUnit + "," : "")
              + ((orgName != null) ? "O=" + orgName + "," : "")
              + ((location != null) ? "L=" + location + "," : "")
              + ((state != null) ? "ST=" + state + "," : "")
              + ((country != null) ? "C=" + country + "," : "");

        if (distNameStr.endsWith(",")) {
            distNameStr = distNameStr.substring(0, distNameStr.length() - 1);
        }

        return distNameStr;
    }
}
