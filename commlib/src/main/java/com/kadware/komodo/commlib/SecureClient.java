/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.commlib;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SecureClient {

    /**
     * Provides useful information resulting from the send() request
     */
    public static class ResultFromSend {
        public final int _responseCode;         //  HTTP response code
        public final String _responseMessage;   //  The message (if any) sent along with the code
        public final byte[] _responseStream;    //  The bytes read from either the output or the error stream

        ResultFromSend(
            final int responseCode,
            final String responseMessage,
            final byte[] responseStream
        ) {
            _responseCode = responseCode;
            _responseMessage = responseMessage;
            _responseStream = responseStream;
        }
    }

    /**
     * Dummy host name verifier to keep SSL happy
     */
    private static class DummyHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(
            String hostname,
            SSLSession session
        ) {
            return true;
        }
    }

    /**
     * Dummy trust manager to keep SSL happy
     */
    private static class DummyTrustManager implements X509TrustManager {

        @Override public X509Certificate[] getAcceptedIssuers() { return null; }
        @Override public void checkClientTrusted(X509Certificate[] certificates, String params) { }
        @Override public void checkServerTrusted(X509Certificate[] certificates, String params) { }
    }

    /**
     * Class for header request properties
     */
    private static class RequestProperty {

        public final String _key;
        public final String _value;

        RequestProperty(
            final String key,
            final String value
        ) {
            _key = key;
            _value = value;
        }
    }

    public static final Logger LOGGER = LogManager.getLogger(SecureClient.class);

    private final List<RequestProperty> _properties = new LinkedList<>();   //  cookies, session ids, etc
    public final int _portNumber;                                           //  TCP port number for connection
    public final String _urlString;                                         //  URL for connection


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Given a response in the form of an InputStream, we convert that content to a byte array and return it.
     */
    private byte[] getResponseStreamBytes(
        final InputStream stream
    ) throws IOException {
        if (stream == null) {
            return new byte[0];
        } else {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                DataInputStream dataInputStream = new DataInputStream(stream);
                int bufferSize = 32768;
                byte[] buffer = new byte[bufferSize];
                int charsRead;
                while ((charsRead = dataInputStream.read(buffer, 0, bufferSize)) != -1) {
                    outputStream.write(buffer, 0, charsRead);
                }
                return outputStream.toByteArray();
            } catch (IOException ex) {
                LOGGER.catching(ex);
                throw ex;
            }
        }
    }

    /**
     * Writes log entry(ies) displaying the content as hex bytes and ascii characters
     */
    private void logContent(
        final byte[] content
    ) {
        int bytesPerLine = 32;
        if (LOGGER.isInfoEnabled()) {
            for (int cx = 0; cx < content.length; cx += bytesPerLine) {
                StringBuilder sbhex = new StringBuilder();
                StringBuilder sbasc = new StringBuilder();
                for (int cy = 0; cy < bytesPerLine; ++cy) {
                    int cz = cx + cy;
                    if (cz >= content.length) {
                        sbhex.append("   ");
                    } else {
                        sbhex.append(String.format("%02X ", (int)(content[cz])));
                        if ((content[cz] < 0x20) || (content[cz] >= 0x7f)) {
                            sbasc.append('.');
                        } else {
                            sbasc.append((char)content[cz]);
                        }
                    }
                }
                LOGGER.info(String.format("   %s %s", sbhex.toString(), sbasc.toString()));
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * @param urlString IP address or DNS name for the connection
     * @param portNumber port number for the connection
     */
    public SecureClient(
        final String urlString,
        final int portNumber
    ) {
        _urlString = urlString;
        _portNumber = portNumber;
    }

    /**
     * Adds a key-value pair for a request property
     */
    public final void addProperty(
        final String key,
        final String value
    ) {
        _properties.add(new RequestProperty(key, value));
    }

    /**
     * Clear the set of properties
     */
    public final void clearProperties() {
        _properties.clear();
    }

    /**
     * General engine for sending/receiving traffic.
     * @param operation     The particular REST operation the caller wants to invoke
     * @param path          path for the activity
     * @param content       if not null, it is the data to be sent.
     * @return resulting traffic from the requested action (applies only to "GET")
     */
    public ResultFromSend send(
        final HttpMethod operation,
        final String path,
        final byte[] content
    ) throws IOException,
             KeyManagementException,
             NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getInstance("SSL");
        TrustManager[] trustManagers = new TrustManager[]{new DummyTrustManager()};
        sslContext.init(null, trustManagers, null);

        //	Set up the new connection using a dummy host name verifier
        URL url = new URL("https", _urlString, _portNumber, path.replace(" ", "%20"));
        LOGGER.info(String.format("Sending to URL:%s operation=%s", url, operation.toString()));
        System.out.println(String.format("Sending to URL:%s operation=%s", url, operation.toString()));//TODO remove

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(new DummyHostnameVerifier());
        connection.setDoInput(true);
        connection.setDoOutput(content != null);

        //  Set up properties
        for (RequestProperty property : _properties) {
            LOGGER.info(String.format("  Property %s=%s", property._key, property._value));
            connection.addRequestProperty(property._key, property._value);
        }

        //  Set method
        connection.setRequestMethod(operation.toString());

        //	Send content, if we have any
        if (content != null) {
            logContent(content);
            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            output.write(content);
        }

        int responseCode = connection.getResponseCode();
        String responseMessage = connection.getResponseMessage();
        LOGGER.info(String.format("Result=%d:%s", responseCode, responseMessage));
        byte[] response;
        if (responseCode >= 300) {
            response = getResponseStreamBytes(connection.getErrorStream());
        } else {
            response = getResponseStreamBytes(connection.getInputStream());
        }

        LOGGER.info(String.format("<--%s", new String(response)));
        return new ResultFromSend(responseCode, responseMessage, response);
    }

    /**
     * Implements a REST DELETE function
     * @param path REST path
     */
    public ResultFromSend sendDelete(
        final String path
    ) throws IOException,
             KeyManagementException,
             NoSuchAlgorithmException {
        return send(HttpMethod.DELETE, path, null);
    }

    /**
     * Implements a REST GET function
     * @param path REST path
     */
    public ResultFromSend sendGet(
        final String path
    ) throws IOException,
             KeyManagementException,
             NoSuchAlgorithmException {
        return send(HttpMethod.GET, path, null);
    }

    /**
     * Implements a REST POST operation
     * @param path REST path
     * @param content content to be applied to the path
     */
    public ResultFromSend sendPost(
        final String path,
        final byte[] content
    ) throws IOException,
             KeyManagementException,
             NoSuchAlgorithmException {
        return send(HttpMethod.POST, path, content);
    }

    /**
     * Implements a REST PUT operation
     * @param path REST path
     * @param content content to be applied to the path
     */
    public ResultFromSend sendPut(
        final String path,
        final byte[] content
    ) throws IOException,
             KeyManagementException,
             NoSuchAlgorithmException {
        return send(HttpMethod.PUT, path, content);
    }
}
