/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which implements the functionality necessary for an architecturally defined system control facility.
 * Our design stipulates the existence of exactly one of these in a proper configuration.
 * This object manages all console communication, and implements the system-wide dayclock.
 * It is also responsible for creating and managing the partition data bank which is used by the operating system.
 */
@SuppressWarnings("Duplicates")
public class SystemProcessor extends Processor {

    //  Tape Boot Procedure:
    //      A starting IP is specified, along with the device upon which the boot tape is mounted,
    //      and the disk device on which the DRS pack is mounted.
    //      The tape path is selected consisting of:
    //          the tape device on which the boot tape is mounted
    //          a channel module connected to the tape device
    //          the IOP which contains the channel module
    //      A memory block of 1792 words is allocated to contain the loader bank
    //      The first block is read from the tape into the loader bank
    //      A memory block is allocated to contain the configuration data bank, and is populated
    //      A memory block is allocated to contain the initial level 0 BDT, which contains the
    //          interrupt vector for the IPL interrupt, which contains a GOTO which transfers
    //          control to the loader bank
    //      The ICS BReg and XReg are initialized to refer to the interrupt vectors in the initial level 0 BDT,
    //          BR2 is initialized to refer to the configuration data bank,
    //          and the IP is started (which causes it to generate a class 29 interrupt - the IPL interrupt).
    //
    //  Disk Boot Procedure:
    //      A starting IP is specified, along with the device upon which the relevant DRS pack is mounted.
    //      The disk path is selected consisting of:
    //          the disk device on which the DRS pack is mounted
    //          a channel module connected to the disk device
    //          the IOP which contains the channel module
    //      A memory block of 1792 words is allocated to contain the loader bank
    //      The first one or two blocks (depending on block size) are read from the DRS pack into the loader bank
    //      A memory block is allocated to contain the configuration data bank, and is populated
    //      A memory block is allocated to contain the initial level 0 BDT, which contains the
    //          interrupt vector for the IPL interrupt, which contains a GOTO which transfers
    //          control to the loader bank
    //      The ICS BReg and XReg are initialized to refer to the interrupt vectors in the initial level 0 BDT,
    //          BR2 is initialized to refer to the configuration data bank,
    //          and the IP is started (which causes it to generate a class 29 interrupt - the IPL interrupt).
    //
    //  Notes:
    //      Loader code must know how many OS banks exist, and how big they are
    //      The loader is responsible for creating the Level 0 BDT (at a minimum)
    //      At some point, the loader must send UPI interrupts to the other IPs in the partition.
    //          They will have no idea where the Level 0 BDT is, and cannot properly handle the interrupt.
    //          So - the invoking processor stores the absolute address of the level 0 BDT in the mail slots
    //          for the various IPs, and the UPI handler code in the IP reads that, and sets the Level 0 BDT
    //          register accordingly before raising the Initial (class 30) interrupt.

    private static final Logger LOGGER = LogManager.getLogger(SystemProcessor.class);
    private static SystemProcessor _instance = null;

    private int _port;
    private long _jumpKeys = 0;
    private HttpServer _httpServer = null;
//    private SSLServerSocket _serverSocket = null;
//    private Thread _serverThread = null;


//    //  ----------------------------------------------------------------------------------------------------------------------------
//    //  Internal class for server async thread - started by the main async worker thread
//    //  ----------------------------------------------------------------------------------------------------------------------------
//
//    private class Server implements Runnable {
//
//        @Override
//        public void run() {
//            //  Set up socket
//            System.out.println("Starting SystemProcessor server");
//            try {
//                _serverSocket = (SSLServerSocket) SSLServerSocketFactory.getDefault().createServerSocket(_port);
//                System.out.println(String.join(",", _serverSocket.getEnabledCipherSuites()));
//                System.out.println(String.join(",", _serverSocket.getEnabledProtocols()));
//                System.out.println(String.join(",", _serverSocket.getSupportedCipherSuites()));
//                System.out.println(String.join(",", _serverSocket.getSupportedProtocols()));
//            } catch (IOException ex) {
//                LOGGER.catching(ex);
//                System.out.println("Caught " + ex.getMessage());
//                System.out.println("Cannot start SystemProcessor secure server");
//                return;
//            }
//
//            try {
//                Charset encoding = StandardCharsets.UTF_8;
//                while (!_workerTerminate) {
//                    Socket socket = _serverSocket.accept();
//                    System.out.println("Connected " + socket.getRemoteSocketAddress().toString());
//
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), encoding.name()));
//                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), encoding.name()));
//                    getHeaderLines(reader).forEach(System.out::println);
//
//                    socket.getOutputStream().write(new String("Go away, pinhead\n").getBytes());
//                    socket.getOutputStream().flush();
//                    socket.close();
//                }
//            } catch (IOException ex) {
//                //  thrown by accept() - means we are shutting down
//            }
//
//            LOGGER.info("SystemProcessor secure server shutting down");
//        }
//
//        private List<String> getHeaderLines(
//            BufferedReader reader
//        ) throws IOException {
//            List<String> lines = new LinkedList<String>();
//            String str = reader.readLine();
//            while (!str.isEmpty()) {
//                lines.add(str);
//                str = reader.readLine();
//            }
//            return lines;
//        }
//    }

    private static class ServerRequestHandler implements HttpHandler {
        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            String response = "Go away, pinhead";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param name node name of the SP
     */
    SystemProcessor(
        final String name,
        final int port
    ) {
        super(Type.SystemProcessor, name, InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
        _port = port;
        synchronized (SystemProcessor.class) {
            LOGGER.error("Attempted to instantiate more than one SystemProcessor");
            assert(_instance == null);
            _instance = this;
        }
    }

    /**
     * constructor for testing
     */
    public SystemProcessor() {
        super(Type.SystemProcessor, "SP0", InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
        _instance = this;
    }

    /**
     * Retrieve singleton instance
     */
    public static SystemProcessor getInstance() {
        return _instance;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Establishes and populates a communications area in one of the configured MSPs.
     * Should be invoked after clearing the various processors and before IPL.
     * The format of the communications area is as follows:
     *      +----------+----------+----------+----------+----------+----------+
     * +0   |          |          |          |            #Entries            |
     *      +----------+----------+----------+----------+----------+----------+
     *      |                           First Entry                           |
     * +1   |  SOURCE  |   DEST   |          |          |          |          |
     *      +----------+----------+----------+----------+----------+----------+
     * +2   |                           First Entry                           |
     * +3   |       Area for communications from source to destination        |
     *      +----------+----------+----------+----------+----------+----------+
     *      |                       Subsequent Entries                        |
     *      |                               ...                               |
     *      +----------+----------+----------+----------+----------+----------+
     * #ENTRIES:  Number of 3-word entries in the table.
     * SOURCE:    UPI Index of processor sending the interrupt
     * DEST:      UPI Index of processor to which the interrupt is sent
     *
     * It should be noted that not every combination of UPI index pairs are necessary,
     * as not all possible paths between types of processors are supported, or implemented.
     * Specifically, we allow interrupts from SPs and IPs to IOPs, as well as the reverse,
     * and we allow interrupts from SPs to IPs and the reverse.
     */
    //TODO should this whole area just be part of the partition data bank?
    private void establishCommunicationsArea(
        final MainStorageProcessor msp,
        final int segment,
        final int offset
    ) throws AddressingExceptionInterrupt {
        //  How many communications slots do we need to create?
        List<Processor> processors = InventoryManager.getInstance().getProcessors();

        int iopCount = 0;
        int ipCount = 0;
        int spCount = 0;

        for (Processor processor : processors) {
            switch (processor._Type) {
                case InputOutputProcessor:
                    iopCount++;
                    break;
                case InstructionProcessor:
                    ipCount++;
                    break;
                case SystemProcessor:
                    spCount++;
                    break;
            }
        }

        //  slots from IPs and SPs to IOPs, and back
        int entries = 2 * (ipCount + spCount) * iopCount;

        //  slots from SPs to IPs
        entries += 2 * spCount * ipCount;

        int size = 1 + (3 * entries);
        ArraySlice commsArea = new ArraySlice(msp.getStorage(segment), offset, size);
        commsArea.clear();

        commsArea.set(0, entries);
        int ax = 1;

        for (Processor source : processors) {
            if ((source._Type == Type.InstructionProcessor)
                || (source._Type == Type.SystemProcessor)) {
                for (Processor destination : processors) {
                    if (destination._Type == Type.InputOutputProcessor) {
                        Word36 w = new Word36();
                        w.setS1(source._upiIndex);
                        w.setS2(destination._upiIndex);
                        commsArea.set(ax, w.getW());
                        ax += 3;

                        w.setS1(destination._upiIndex);
                        w.setS2(source._upiIndex);
                        commsArea.set(ax, w.getW());
                        ax += 3;
                    } else if ((source._Type == Type.SystemProcessor)
                               && (destination._Type == Type.InstructionProcessor)) {
                        Word36 w = new Word36();
                        w.setS1(source._upiIndex);
                        w.setS2(destination._upiIndex);
                        commsArea.set(ax, w.getW());
                        ax += 3;

                        w.setS1(destination._upiIndex);
                        w.setS2(source._upiIndex);
                        commsArea.set(ax, w.getW());
                        ax += 3;
                    }
                }
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Implementations / overrides of abstract base methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Clears the processor - actually, we never get cleared
     */
    @Override public void clear() {}

    /**
     * SPs have no ancestors
     * @param ancestor candidate ancestor
     * @return always false
     */
    @Override public final boolean canConnect(Node ancestor) { return false; }

    /**
     * For debugging
     * @param writer destination for output
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Async worker thread and its sub methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        LOGGER.info(_name + " worker thread starting");
        runInit();

        while (!_workerTerminate) {
            if (!runLoop()) {
                try {
                    synchronized (this) {
                        wait(25);
                    }
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }
        }

        runTerm();
        LOGGER.info(_name + " worker thread terminating");
    }

    /**
     * Any one-time things needing done by the async worker on startup
     */
    private void runInit() {
//        _serverThread = new Thread(new Server());
//        _serverThread.start();
        try {
            _httpServer = HttpServer.create(new InetSocketAddress(_port), 0);
            _httpServer.createContext("/", new ServerRequestHandler());
            _httpServer.setExecutor(Executors.newCachedThreadPool());
            _httpServer.start();
        } catch (IOException ex) {
            LOGGER.catching(ex);
            System.out.println("Caught " + ex.getMessage());
            System.out.println("Cannot start SystemProcessor secure server");
        }
    }

    /**
     * Performs one iteration of the worker's work.
     * @return true if we did something, indicating we should be invoked again without delay
     */
    private boolean runLoop() {
        boolean didSomething = false;

        //  Check UPI ACKs and SENDs
        //  ACKs mean we can send another IO
        synchronized (_upiPendingAcknowledgements) {
            for (Processor source : _upiPendingAcknowledgements) {
                //TODO
                LOGGER.error(String.format("%s received a UPI ACK from %s", _name, source._name));
                didSomething = true;
            }
            _upiPendingAcknowledgements.clear();
        }

        //  SENDs mean an IO is completed
        synchronized (_upiPendingInterrupts) {
            for (Processor source : _upiPendingInterrupts) {
                //TODO
                LOGGER.error(String.format("%s received a UPI interrupt from %s", _name, source._name));
                didSomething = true;
            }
            _upiPendingInterrupts.clear();
        }

        return didSomething;
    }

    /**
     * Any one-time things needing done by the async worker on termination
     */
    private void runTerm() {
//        if (_serverSocket != null) {
//            try {
//                _serverSocket.close();
//            } catch (IOException ex) {
//                LOGGER.catching(ex);
//            }
//
//            _serverSocket = null;
//        }
    }


    //  ------------------------------------------------------------------------
    //  Public methods - for other processors to invoke
    //  Mostly for InstructionProcessor's SYSC instruction
    //  ------------------------------------------------------------------------

    /**
     * Represents console input
     */
    static class ConsoleInput {
        public final long _identifier;  //  zero for unsolicited input, messageIdentifier from SendReadReply if response
        public final String _message;

        ConsoleInput(
            final long identifier,
            final String message
        ) {
            _identifier = identifier;
            _message = message;
        }

        ConsoleInput(
            final String message
        ) {
            _identifier = 0;
            _message = message;
        }
    }

    ConsoleInput consolePoll() {
        return null;//TODO
    }

    void consoleReset() {
        //TODO
    }

    void consoleSendReadOnlyMessage(
        final long messageIdentifier,
        final String message
    ) {
        //TODO
    }

    void consoleSendReadReplyMessage(
        final long messageIdentifier,
        final String message,
        final int replyMaxCharacters
    ) {
        //TODO
    }

    void consoleSendSystemMessage(
        final String message1,
        final String message2
    ) {
        //TODO
    }

    long jumpKeysGet() {
        return _jumpKeys;
    }

    void jumpKeySet(
        final int key,          //  1-biased
        final boolean value
    ) {
        if (value) {
            _jumpKeys |= 1L << (36 - key);
        } else {
            _jumpKeys &= (1L << (36 - key)) ^ 0_777777_777777L;
        }
    }

    void jumpKeysSet(
        final long value
    ) {
        _jumpKeys = value;
    }

//package httpsTest;
//
//import com.sun.net.httpserver.Headers;
//import com.sun.net.httpserver.HttpContext;
//import com.sun.net.httpserver.HttpExchange;
//import com.sun.net.httpserver.HttpHandler;
//import com.sun.net.httpserver.HttpsConfigurator;
//import com.sun.net.httpserver.HttpsParameters;
//import com.sun.net.httpserver.HttpsServer;
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.net.InetAddress;
//import java.net.InetSocketAddress;
//import java.net.HttpURLConnection;
//import java.net.Socket;
//import java.security.KeyStore;
//import java.security.KeyStoreException;
//import java.security.NoSuchAlgorithmException;
//import java.security.Principal;
//import java.security.PrivateKey;
//import java.security.UnrecoverableEntryException;
//import java.security.cert.Certificate;
//import java.security.cert.X509Certificate;
//import java.util.ArrayList;
//import java.util.GregorianCalendar;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//import javax.net.ssl.KeyManager;
//import javax.net.ssl.SSLContext;
//import javax.net.ssl.SSLEngine;
//import javax.net.ssl.SSLParameters;
//import javax.net.ssl.TrustManagerFactory;
//import javax.net.ssl.X509ExtendedKeyManager;
//import sun.security.tools.keytool.CertAndKeyGen;
//import sun.security.x509.CertificateExtensions;
//import sun.security.x509.DNSName;
//import sun.security.x509.Extension;
//import sun.security.x509.GeneralName;
//import sun.security.x509.GeneralNames;
//import sun.security.x509.IPAddressName;
//import sun.security.x509.KeyIdentifier;
//import sun.security.x509.KeyUsageExtension;
//import sun.security.x509.SubjectAlternativeNameExtension;
//import sun.security.x509.SubjectKeyIdentifierExtension;
//import sun.security.x509.X500Name;
//
//    /**
//     *
//     * @author kduncan
//     */
//    public class httpsTest {
//
//        /**
//         * Configurator for the HTTPS Server
//         */
//        private static class Configurator extends HttpsConfigurator {
//            public Configurator(
//                SSLContext context
//            ) {
//                super(context);
//            }
//
//            @Override
//            public void configure(
//                HttpsParameters params
//            ) {
//                try {
//                    SSLContext sslContext = SSLContext.getDefault();
//                    SSLEngine sslEngine = sslContext.createSSLEngine();
//                    params.setNeedClientAuth(true);
//                    params.setCipherSuites(sslEngine.getEnabledCipherSuites());
//                    params.setProtocols(sslEngine.getEnabledProtocols());
//
//                    SSLParameters defaultSSLParameters = sslContext.getDefaultSSLParameters();
//                    params.setSSLParameters(defaultSSLParameters);
//                } catch (Exception e) {
//                    _logger.catching(e);
//                }
//            }
//        }
//
//        private static class CustomKeyManager extends X509ExtendedKeyManager {
//
//            private final String _keyPassword;
//            private final KeyStore _keyStore;
//            private final String _storePassword;
//
//            /**
//             * Constructor
//             * @param keyStore the KeyStore containing the certificates to use as keys
//             * @param storePassword the password to use for the keystore in general
//             * @param keyPassword the password to use for any private entries
//             */
//            CustomKeyManager(
//                final KeyStore keyStore,
//                final String storePassword,
//                final String keyPassword
//            ) {
//                _keyStore = keyStore;
//                _keyPassword = keyPassword;
//                _storePassword = storePassword;
//            }
//
//            /**
//             * Retrieve the current preferred alias
//             * @return
//             */
//            public String getPreferredAlias() {
//                return DEFAULT_ALIAS;
//            }
//
////        /**
////         * Change the current preferred alias
////         * @param preferredAlias
////         */
////        public void setPreferredAlias(
////            final String preferredAlias
////        ) {
////            _preferredAlias = preferredAlias;
////        }
//
//            /**
//             * For HTTPS clients only - we're not a client, so we don't use this
//             * <p>
//             * @param keyType irrelevant
//             * @param issuers irrelevant
//             * @param socket irrelevant
//             * <p>
//             * @return null
//             */
//            @Override
//            public String chooseClientAlias(
//                final String[] keyType,
//                final Principal[] issuers,
//                final Socket socket
//            ) {
//                return null;
//            }
//
//            /**
//             * For HTTPS clients only - we're not a client, so we don't use this
//             * <p>
//             * @param keyType irrelevant
//             * @param issuers irrelevant
//             * @param socket irrelevant
//             * <p>
//             * @return null
//             */
//            @Override
//            public String chooseEngineClientAlias(
//                final String[] keyType,
//                final Principal[] issuers,
//                final SSLEngine engine
//            ) {
//                return null;
//            }
//
//            /**
//             * Return the preferred alias if it exists in the keyStore; otherwise return the default.
//             * @param keyType must be "RSA"
//             * @param issuers ignored
//             * @param socket indicates which socket the request came from
//             * @return
//             */
//            @Override
//            public String chooseEngineServerAlias(
//                final String keyType,
//                final Principal[] issuers,
//                final SSLEngine engine
//            ) {
//                return DEFAULT_ALIAS;
//            }
//
//            /**
//             * Return the preferred alias if it exists in the keyStore; otherwise return the default.
//             * @param keyType must be "RSA"
//             * @param issuers ignored
//             * @param socket indicates which socket the request came from
//             * @return
//             */
//            @Override
//            public String chooseServerAlias(
//                final String keyType,
//                final Principal[] issuers,
//                final Socket socket
//            ) {
//                return DEFAULT_ALIAS;
//            }
//
//            /**
//             * Returns certificate chain with the certificate indicated by the alias first, then by subsequent signing certificates
//             * <p>
//             * @param alias alias of certificate of interest
//             * <p>
//             * @return chain of certificates
//             */
//            @Override
//            public X509Certificate[] getCertificateChain(
//                final String alias
//            ) {
//                try {
//                    Certificate[] certs = _keyStore.getCertificateChain(alias);
//                    X509Certificate[] result = new X509Certificate[certs.length];
//                    for (int cx = 0; cx < certs.length; ++cx) {
//                        result[cx] = (X509Certificate) certs[cx];
//                    }
//                    return result;
//                } catch (KeyStoreException e) {
//                    _logger.catching(e);
//                }
//
//                return new X509Certificate[0];
//            }
//
//            /**
//             * For HTTPS clients only - we're not a client, so we don't use this
//             * <p>
//             * @param keyType irrelevant
//             * @param issuers irrelevant
//             * <p>
//             * @return
//             */
//            @Override
//            public String[] getClientAliases(
//                final String keyType,
//                final Principal[] issuers
//            ) {
//                return null;
//            }
//
//            /**
//             * Retrieves the private key for the indicated alias
//             * <p>
//             * @param alias indicates the key-pair of interest
//             * <p>
//             * @return
//             */
//            @Override
//            public PrivateKey getPrivateKey(
//                final String alias
//            ) {
//                try {
//                    KeyStore.ProtectionParameter protection = new KeyStore.PasswordProtection(_keyPassword.toCharArray());
//                    KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) _keyStore.getEntry(alias, protection);
//                    return pkEntry.getPrivateKey();
//                } catch (KeyStoreException
//                    | NoSuchAlgorithmException
//                    | UnrecoverableEntryException e) {
//                    _logger.catching(e);
//                    return null;
//                }
//            }
//
//            /**
//             * Retrieves the set of aliases which can be used for an HTTPS server
//             * <p>
//             * @param keyType must be "RSA"
//             * @param issuers ignored
//             * <p>
//             * @return
//             */
//            @Override
//            public String[] getServerAliases(
//                final String keyType,
//                final Principal[] issuers
//            ) {
//                try {
//                    ArrayList<String> list = new ArrayList<>();
//                    KeyStore.ProtectionParameter protection = new KeyStore.PasswordProtection(_keyPassword.toCharArray());
//                    while (_keyStore.aliases().hasMoreElements()) {
//                        String alias = _keyStore.aliases().nextElement();
//                        KeyStore.Entry entry = _keyStore.getEntry(alias, protection);
//                        if (entry instanceof KeyStore.PrivateKeyEntry) {
//                            list.add(alias);
//                        }
//                    }
//                    return (String[]) list.toArray();
//                } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
//                    return null;
//                }
//            }
//        }
//
//        /**
//         * Default handler for any incoming request not specifically directed to the VASA provider
//         */
//        private static class DefaultHandler implements HttpHandler {
//            @Override
//            public void handle(
//                HttpExchange exchange
//            ) {
//                _logger.trace("Local Address:  "
//                              + exchange.getLocalAddress().getAddress().getCanonicalHostName()
//                              + ":"
//                              + exchange.getLocalAddress().getPort());
//                _logger.trace("Remote Address: "
//                              + exchange.getRemoteAddress().getAddress().getCanonicalHostName()
//                              + ":"
//                              + exchange.getRemoteAddress().getPort());
//                _logger.trace("Protocol:       " + exchange.getProtocol());
//                _logger.trace("Request Method: " + exchange.getRequestMethod());
//                _logger.trace("Request URI:    " + exchange.getRequestURI());
//
//                Headers h = exchange.getRequestHeaders();
//                for (String key : h.keySet()) {
//                    for (String value : h.get(key)) {
//                        System.out.println(String.format("  %s:%s", key, value));
//                    }
//                }
//
//                StringBuilder sboct = new StringBuilder();
//                StringBuilder sbasc = new StringBuilder();
//                try {
//                    InputStream is = exchange.getRequestBody();
//                    int bc = 0;
//                    while (true) {
//                        int bite = is.read();
//                        if (bite < 0) {
//                            break;
//                        }
//                        sboct.append(String.format(" %02X", bite));
//                        if ((bite >= 32) && (bite < 255)) {
//                            sbasc.append(Character.toChars(bite));
//                        } else {
//                            sbasc.append('.');
//                        }
//                        ++bc;
//
//                        if (bc == 16) {
//                            System.out.println(sboct.toString() + "   " + sbasc.toString());
//                            bc = 0;
//                            sbasc = new StringBuilder();
//                            sboct = new StringBuilder();
//                        }
//                    }
//                } catch (IOException ex) {
//                    if (sboct.length() > 0) {
//                        System.out.println(sboct.toString() + "   " + sbasc.toString());
//                    }
//                    System.out.println("caught IOException");
//                }
//
//                try {
//                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, RESPONSE_BYTES.length);
//                    OutputStream outputStream = exchange.getResponseBody();
//                    outputStream.write(RESPONSE_BYTES);
//                    outputStream.close();
//                } catch (Exception e) {
//                    _logger.catching(e);
//                }
//
//                exchange.close();
//            }
//        }
//
//        private static class Logger {
//            public void catching(
//                Throwable t
//            ) {
//                System.out.println("Caught:" + t.toString());
//                System.out.println("      :" + t.getMessage());
//                t.printStackTrace();
//            }
//
//            public void error(
//                final String msg
//            ) {
//                System.out.println(msg);
//            }
//
//            public void trace(
//                final String msg
//            ) {
//                System.out.println(msg);
//            }
//        }
//
//        private static Logger _logger = new Logger();
//
//        private static final String RESPONSE_STRING = "The FOO Hit the FAN\r\nYou Win!";
//        private static final byte[] RESPONSE_BYTES = RESPONSE_STRING.getBytes();
//
//        private static boolean _done = false;
//
//        private static final int CONNECTION_BACKLOG             = 32;
//        private static final int HTTPS_PORT_NUMBER              = 9443;
//        private static final String KEY_ENTRY_PASSWORD          = "pv3horsehammerball";
//        private static final String KEY_STORE_FILE_NAME         = "jssecacerts";
//        private static final String KEY_STORE_PASSWORD          = "pv3snailrocketgag";
//        private static final String KEY_STORE_TYPE              = "JKS";
//        private static final String KEY_MANAGER_TYPE            = "SunX509";
//        private static final String TRUST_MANAGER_TYPE          = "SunX509";
//
//        private static final String SSL_TYPE                    = "TLS";
//        private static final String KEY_TYPE                    = "RSA";
//        private static final String ALGORITHM                   = "SHA256WithRSA";
//        private static final int KEY_SIZE                       = 2048;
//        private static final long VALIDITY                      = 3650 * 24 * 60 * 60;  //  3650 days, expressed in seconds
//
//        private static final String DEFAULT_ALIAS               = "default";
//        private static final String DEFAULT_CERT_X500NAME_OU    = "VASAProvider";       //  organizational unit
//        private static final String DEFAULT_CERT_X500NAME_DC    = "Pivot3";             //  domain component
//        private static final String DEFAULT_CERT_X500NAME_L     = "Louisville";         //  locality name
//        private static final String DEFAULT_CERT_X500NAME_ST    = "Colorado";           //  state or province name
//        private static final String DEFAULT_CERT_X500NAME_C     = "US";                 //  country name
//
//        /**
//         * @param args the command line arguments
//         */
//        public static void main(String[] args) {
//            try {
//                KeyStore keyStore = KeyStore.getInstance(KEY_STORE_TYPE);
//                keyStore.load(null, KEY_STORE_PASSWORD.toCharArray());
//                createSelfSignedCertificate(keyStore);
//
//                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TRUST_MANAGER_TYPE);
//                trustManagerFactory.init(keyStore);
//
//                KeyManager[] newKeyManagers = new KeyManager[1];
//                newKeyManagers[0] = new CustomKeyManager(keyStore, KEY_STORE_PASSWORD, KEY_ENTRY_PASSWORD);
//
//                SSLContext sslContext = SSLContext.getInstance(SSL_TYPE);
//                sslContext.init(newKeyManagers, trustManagerFactory.getTrustManagers(), null);
//
//                HttpsServer server = HttpsServer.create();
//                server.setHttpsConfigurator(new Configurator(sslContext));
//                HttpContext defaultContext = server.createContext("/", new DefaultHandler());
//
//                InetAddress inetAddress = InetAddress.getLocalHost();
//                InetSocketAddress isAddr = new InetSocketAddress(inetAddress, HTTPS_PORT_NUMBER);
//                server.bind(isAddr, CONNECTION_BACKLOG);
//                server.start();
//                while (!_done) {
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException ex) {
//                        //????
//                    }
//                }
//            } catch (Throwable t) {
//                _logger.error("Throwable:" + t.toString());
//                for (StackTraceElement element : t.getStackTrace()) {
//                    _logger.error("   " + element.toString());
//                }
//            }
//        }
//
//        public static X509Certificate createSelfSignedCertificate(
//            KeyStore keyStore
//        ) throws Exception {
//            if (keyStore.containsAlias(DEFAULT_ALIAS)) {
//                keyStore.deleteEntry(DEFAULT_ALIAS);
//            }
//
//            //  Generate key pair
//            CertAndKeyGen keyPair = new CertAndKeyGen(KEY_TYPE, ALGORITHM);
//            keyPair.generate(KEY_SIZE);
//
//            //  Generate certificate with appropriate extensions
//            String distNameStr = createDistinguisedNameString("IP:10.128.72.186",
//                                                              DEFAULT_CERT_X500NAME_OU,
//                                                              DEFAULT_CERT_X500NAME_DC,
//                                                              DEFAULT_CERT_X500NAME_L,
//                                                              DEFAULT_CERT_X500NAME_ST,
//                                                              DEFAULT_CERT_X500NAME_C);
//            X500Name distinguishedName = new X500Name(distNameStr);
//
//            //  Set up extensions
//            CertificateExtensions extensions = new CertificateExtensions();
//
//            //  Always include KeyUsage.
//            //  Note that the KeyUsageExtension constructor does not provide a way to indicate criticality (which should be true),
//            //  so once we've created the object, we have to re-create with the generic constructor to set that flag true.
//            boolean[] flags = new boolean[9];
//            flags[5] = true;    //  keyCertSign flag
//            flags[6] = true;    //  Crl_Sign flag
//            KeyUsageExtension kuExtension = new KeyUsageExtension(flags);
//            Extension generalKUExtension = Extension.newExtension(kuExtension.getExtensionId(), true, kuExtension.getValue());
//            extensions.set(KeyUsageExtension.NAME, generalKUExtension);
//
//            //  Did the caller specify a SAN?  If so, create a subject-alternative-name extension
////            if (subjectAltName != null) {
////                int cx = subjectAltName.indexOf(':');
////                if (cx < 0) {
////                    throw new InternalErrorException("Invalid subjectAltName");
////                }
////
////                String type = subjectAltName.substring(0, cx);
////                String value = subjectAltName.substring(cx + 1);
////                GeneralNames generalNames = new GeneralNames();
////                switch (type.toLowerCase()) {
////                    case "ip":
////                        generalNames.add(new GeneralName(new IPAddressName(value)));
////                        break;
////                    case "dns":
////                        generalNames.add(new GeneralName(new DNSName(value)));
////                        break;
////                    default:
////                        throw new InternalErrorException("Invalid SAN type");
////                }
////                SubjectAlternativeNameExtension sanExtension = new SubjectAlternativeNameExtension(false, generalNames);
////                extensions.set(SubjectAlternativeNameExtension.NAME, sanExtension);
////            }
//
//            //  Always include key identifier
//            KeyIdentifier keyIdentifier = new KeyIdentifier(keyPair.getPublicKey());
//            SubjectKeyIdentifierExtension skiExtension = new SubjectKeyIdentifierExtension(keyIdentifier.getIdentifier());
//            extensions.set(SubjectKeyIdentifierExtension.NAME, skiExtension);
//
//            //  Create the X509Certificate object
//            X509Certificate[] certificateChain = new X509Certificate[1];
//            certificateChain[0] = keyPair.getSelfCertificate(distinguishedName,
//                                                             new GregorianCalendar().getTime(),
//                                                             VALIDITY,
//                                                             extensions);
//
//            //  Store private key and the certificate chain (consisting of the single certificate) in the keyStore
//            String certPassword = KEY_ENTRY_PASSWORD;
//            keyStore.setKeyEntry(DEFAULT_ALIAS, keyPair.getPrivateKey(), certPassword.toCharArray(), certificateChain);
//
//            //  Return the single certificate
//            return certificateChain[0];
//        }
//
//        public static String createDistinguisedNameString(
//            final String commonName,
//            final String orgUnit,
//            final String orgName,
//            final String location,
//            final String state,
//            final String country
//        ) {
//            String distNameStr
//                = ((commonName != null) ? "CN=" + commonName + "," : "")
//                  + ((orgUnit != null) ? "OU=" + orgUnit + "," : "")
//                  + ((orgName != null) ? "O=" + orgName + "," : "")
//                  + ((location != null) ? "L=" + location + "," : "")
//                  + ((state != null) ? "ST=" + state + "," : "")
//                  + ((country != null) ? "C=" + country + "," : "");
//
//            if (distNameStr.endsWith(",")) {
//                distNameStr = distNameStr.substring(0, distNameStr.length() - 1);
//            }
//
//            return distNameStr;
//        }
//    }
}
