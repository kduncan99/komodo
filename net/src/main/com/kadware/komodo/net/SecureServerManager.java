/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.net;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class SecureServerManager {

    private final String _ksFileName;
    private final String _ksPassword;
    private SSLServerSocketFactory _socketFactory;

    public SecureServerManager(
        final String keyStoreFileName,
        final String keyStorePassword
    ) {
        _ksFileName = keyStoreFileName;
        _ksPassword = keyStorePassword;
    }

    public SSLServerSocket createSocket(
        final int port
    ) throws IOException {
        return (SSLServerSocket) _socketFactory.createServerSocket(port);
    }

    public void setup(
    ) throws CertificateException,
             IOException,
             KeyManagementException,
             KeyStoreException,
             NoSuchAlgorithmException,
             UnrecoverableKeyException {
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(_ksFileName), _ksPassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("NewSunX509");
        kmf.init(ks, _ksPassword.toCharArray());
        KeyManager[] kms = kmf.getKeyManagers();

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ks);
        TrustManager[] tms = tmf.getTrustManagers();

        SSLContext ctx = SSLContext.getInstance("TLS");
        ctx.init(kms, tms, null);
        _socketFactory = ctx.getServerSocketFactory();
    }
}
