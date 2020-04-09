/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.KomodoAppender;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.commlib.ConsoleInputMessage;
import com.kadware.komodo.commlib.ConsoleReadOnlyMessage;
import com.kadware.komodo.commlib.ConsoleStatusMessage;
import com.kadware.komodo.commlib.HttpMethod;
import com.kadware.komodo.commlib.SecureServer;
import com.kadware.komodo.commlib.JumpKeys;
import com.kadware.komodo.commlib.PollResult;
import com.kadware.komodo.commlib.SystemLogEntry;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class which implements the functionality necessary for a system console.
 * This variation implements a REST server interface, providing all the functionality required of a system console
 * via HTTP / HTTPS REST methods (i.e., DELETE, GET, POST, PUT).
 * Our design provides for multiple clients, but which are not visible as such, to the operating system, which
 * 'sees' our client(s) as one console.
 */
@SuppressWarnings("Duplicates")
public class RESTSystemConsole implements SystemConsole {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  interface for anonymous-class-based client notification
    //  ----------------------------------------------------------------------------------------------------------------------------

    private interface PokeClientFunction {
        void function(final ClientInfo clientInfo);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Information local to each established session
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * ClientInfo - information regarding a particular client
     */
    private static class ClientInfo {

        private final String _clientId;
        private long _lastActivity = System.currentTimeMillis();
        private boolean _isMaster = false;
        private InetSocketAddress _remoteAddress = null;

        //  notification-ish things
        public boolean _inputDelivered = false;
        public boolean _isMasterChanged = false;
        public List<KomodoAppender.LogEntry> _pendingLogEntries = new LinkedList<>();
        public List<ReadOnlyMessage> _pendingReadOnlyMessages = new LinkedList<>();
        public boolean _updatedHardwareConfiguration = false;
        public boolean _updatedJumpKeys = false;
        public boolean _updatedReadReplyMessages = false;
        public boolean _updatedStatusMessage = false;
        public boolean _updatedSystemConfiguration = false;

        ClientInfo(
            final String clientId
        ) {
            _clientId = clientId;
        }

        public void clear() {
            _inputDelivered = false;
            _isMasterChanged = false;
            _pendingLogEntries.clear();
            _pendingReadOnlyMessages.clear();
            _updatedHardwareConfiguration = false;
            _updatedJumpKeys = false;
            _updatedReadReplyMessages = false;
            _updatedStatusMessage = false;
            _updatedSystemConfiguration = false;
        }

        public boolean hasUpdatesForClient() {
            return _inputDelivered
                || _isMasterChanged
                || !_pendingLogEntries.isEmpty()
                || !_pendingReadOnlyMessages.isEmpty()
                || _updatedHardwareConfiguration
                || _updatedJumpKeys
                || _updatedReadReplyMessages
                || _updatedStatusMessage
                || _updatedSystemConfiguration;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final long CLIENT_AGE_OUT_MSECS = 10 * 60 * 1000;        //  10 minutes of no polling ages out a client
    private static final String HTML_FILE_NAME = "systemConsole.html";
    private static final long POLL_WAIT_MSECS = 10000;                      //  10 second (maximum) poll delay
    private static final int MAX_RECENT_READ_ONLY_MESSAGES = 30;            //  max size of container of most-recent RO messages
    private static final long WORKER_PERIODICITY_MSECS = 10000;             //  worker thread does its work every 10 seconds

    private static final String[] _logReportingBlackList = { SystemProcessor.class.getName(),
                                                             RESTSystemConsole.class.getName() };

    private static final Logger LOGGER = LogManager.getLogger(RESTSystemConsole.class);

    private final APIListener _apiListener;
    private final Map<String, ClientInfo> _clientInfos = new HashMap<>();
    private final Configurator _configurator;
    private final JumpKeyPanel _jumpKeyPanel;
    private final String _name;
    private final WebListener _webListener;
    private final String _webRootPath;
    private WorkerThread _workerThread = null;

    //  This is always the latest status message. Clients may pick it up at any time.
    private StatusMessage _latestStatusMessage = null;

    //  Input messages we've received from the console, but which have not yet been delivered to the operating system.
    private final Map<String, InputMessage> _pendingInputMessages = new LinkedHashMap<>();

    //  ReadReply messages which have not yet been replied to
    private final Map<Integer, ReadReplyMessage> _pendingReadReplyMessages = new HashMap<>();

    //  Recent output messages. We preserve a certain number of these so that they can be redisplayed if necessary
    private final Queue<ReadOnlyMessage> _recentReadOnlyMessages = new LinkedList<>() {};


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param name node name of the SP
     */
    RESTSystemConsole(
        final String name,
        final Configurator configurator,
        final String webRootPath,
        final JumpKeyPanel jumpKeyPanel
    ) {
        _name = name;
        _configurator = configurator;
        _jumpKeyPanel = jumpKeyPanel;
        _apiListener = new APIListener(2200);     //  TODO pull portnumber from Configurator hardware configuration
        _webListener = new WebListener(443);
        _webRootPath = webRootPath;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Endpoint handlers, to be attached to the HTTP listeners
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Invalid path handler class
     */
    private class APIDefaultRequestHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            LOGGER.traceEntry(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));
            System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
            try {
                ClientInfo clientInfo = findClient(exchange);
                if (clientInfo == null) {
                    if (!validateCredentials(exchange)) {
                        respondUnauthorized(exchange);
                    } else {
                        respondNoSession(exchange);
                    }
                    return;
                }

                clientInfo._lastActivity = System.currentTimeMillis();
                respondNotFound(exchange, "Path or object does not exist");
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }
        }
    }

    /**
     * Handles requests against the /jumpkeys path
     * GET retrieves the current settings as a WORD36 wrapped in a long.
     * PUT accepts the JK settings as a WORD36 wrapped in a long, and persists them to the singular system jump key panel.
     * Either way, JK36 is in the least-significant bit and JKn is 36-n bits to the left of the LSB.
     */
    private class APIJumpKeysRequestHandler implements HttpHandler {

        private JumpKeys createJumpKeys(
            final long compositeValue
        ) {
            HashMap<Integer, Boolean> map = new HashMap<>();
            long bitMask = 0_400000_000000L;
            for (int jkid = 1; jkid <= 36; jkid++) {
                map.put(jkid, (compositeValue & bitMask) != 0);
                bitMask >>= 1;
            }

            return new JumpKeys(compositeValue, map);
        }

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            LOGGER.traceEntry(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));
            System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
            try {
                ClientInfo clientInfo = findClient(exchange);
                if (clientInfo == null) {
                    respondNoSession(exchange);
                    return;
                }

                clientInfo._lastActivity = System.currentTimeMillis();

                //  For GET - return the settings as both a composite value and a map of individual jump key settings
                if (exchange.getRequestMethod().equalsIgnoreCase(HttpMethod.GET._value)) {
                    JumpKeys jumpKeysResponse = createJumpKeys(_jumpKeyPanel.getJumpKeys().getW());
                    respondWithJSON(exchange, HttpURLConnection.HTTP_OK, jumpKeysResponse);
                    return;
                }

                //  For PUT - accept the input object - if it has a composite value, use that to set the entire jump key panel.
                //  If it has no composite value, but it has component values, use them to individually set the jump keys.
                //  If it has neither, reject the PUT.
                if (exchange.getRequestMethod().equalsIgnoreCase(HttpMethod.PUT._value)) {
                    JumpKeys content;
                    try {
                        content = new ObjectMapper().readValue(exchange.getRequestBody(), JumpKeys.class);
                    } catch (IOException ex) {
                        respondBadRequest(exchange, ex.getMessage());
                        return;
                    }

                    if (content._compositeValue != null) {
                        if ((content._compositeValue < 0) || (content._compositeValue > 0_777777_777777L)) {
                            respondBadRequest(exchange, "Invalid composite value");
                            return;
                        }

                        _jumpKeyPanel.setJumpKeys(new Word36(content._compositeValue));
                        JumpKeys jumpKeysResponse = createJumpKeys(content._compositeValue);
                        respondWithJSON(exchange, HttpURLConnection.HTTP_OK, jumpKeysResponse);
                        return;
                    }

                    if (content._componentValues != null) {
                        for (Map.Entry<Integer, Boolean> entry : content._componentValues.entrySet()) {
                            int jumpKeyId = entry.getKey();
                            if ((jumpKeyId < 1) || (jumpKeyId > 36)) {
                                respondBadRequest(exchange, String.format("Invalid component value jump key id: %d", jumpKeyId));
                                return;
                            }

                            boolean setting = entry.getValue();
                            _jumpKeyPanel.setJumpKey(jumpKeyId, setting);

                            JumpKeys jumpKeysResponse = createJumpKeys(_jumpKeyPanel.getJumpKeys().getW());
                            respondWithJSON(exchange, HttpURLConnection.HTTP_OK, jumpKeysResponse);
                            return;
                        }
                    }

                    respondBadRequest(exchange, "Requires either composite or component values");
                    return;
                }

                //  Neither a GET or a PUT - this is not allowed.
                respondBadMethod(exchange);
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }
        }
    }

    /**
     * Provides a method for injecting input to the system via POST to /message
     */
    private class APIMessageHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            LOGGER.traceEntry(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));
            System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
            try {
                ClientInfo clientInfo = findClient(exchange);
                if (clientInfo == null) {
                    respondNoSession(exchange);
                    return;
                }

                clientInfo._lastActivity = System.currentTimeMillis();

                String method = exchange.getRequestMethod();
                if (!method.equals(HttpMethod.POST._value)) {
                    respondBadMethod(exchange);
                    return;
                }

                boolean collision = false;
                synchronized (_pendingInputMessages) {
                    if (_pendingInputMessages.containsKey(clientInfo._clientId)) {
                        collision = true;
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        ConsoleInputMessage msg = mapper.readValue(exchange.getRequestBody(), ConsoleInputMessage.class);
                        if (msg._messageId != null) {
                            ReadReplyInputMessage rrim = new ReadReplyInputMessage(msg._messageId, msg._text);
                            _pendingInputMessages.put(clientInfo._clientId, rrim);
                        } else {
                            UnsolicitedInputMessage uim = new UnsolicitedInputMessage(msg._text);
                            _pendingInputMessages.put(clientInfo._clientId, uim);
                        }
                    }
                }

                if (collision) {
                    respondWithText(exchange, HttpURLConnection.HTTP_CONFLICT, "Previous input not yet acknowledged");
                } else {
                    respondWithText(exchange, HttpURLConnection.HTTP_CREATED, "");
                }
            } catch (IOException ex) {
                respondBadRequest(exchange, "Badly-formatted body");
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }
        }
    }

    /**
     * Handle a poll request (a GET to /poll).
     * Check to see if there is anything new.  If so, send it.
     * Otherwise, wait for some period of time to see whether anything new pops up.
     */
    private class APIPollRequestHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            LOGGER.traceEntry(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));
            System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
            try {
                ClientInfo clientInfo = findClient(exchange);
                if (clientInfo == null) {
                    respondNoSession(exchange);
                    return;
                }

                clientInfo._lastActivity = System.currentTimeMillis();

                String method = exchange.getRequestMethod();
                if (!method.equals(HttpMethod.GET._value)) {
                    respondBadMethod(exchange);
                    return;
                }

                new APIPollThread(exchange, clientInfo).start();
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }
        }
    }

    /**
     * One of these is spun off for every GET on /poll. We delay returning until either we reach a timeout in which
     * case we return an empty poll result, or there is an update which our particular client would like to know about.
     * Needed so we can go back and service other requests while we're waiting on anything noteworthy to occur.
     */
    class APIPollThread extends Thread {

        private final ClientInfo _clientInfo;
        private final HttpExchange _exchange;

        private APIPollThread(
            final HttpExchange exchange,
            final ClientInfo clientInfo
        ) {
            _clientInfo = clientInfo;
            _exchange = exchange;
        }

        @Override
        public void run() {
            //  Check if there are any updates already waiting for the client to pick up.
            //  If not, go into a wait loop which will be interrupted if any updates eventuate during the wait.
            //  At the end of the wait construct and return a SystemProcessorPollResult object
            synchronized (_clientInfo) {
                if (!_clientInfo.hasUpdatesForClient()) {
                    try {
                        _clientInfo.wait(POLL_WAIT_MSECS);
                    } catch (InterruptedException ex) {
                        //  do nothing
                    }
                }

                Boolean isMaster = null;
                if (_clientInfo._isMasterChanged) {
                    isMaster = _clientInfo._isMaster;
                }

                Long jumpKeyValue = null;
                if (_clientInfo._updatedJumpKeys) {
                    jumpKeyValue = _jumpKeyPanel.getJumpKeys().getW();
                }

                ConsoleStatusMessage latestStatusMessage = null;
                if (_clientInfo._updatedStatusMessage) {
                    latestStatusMessage = new ConsoleStatusMessage(_latestStatusMessage._text);
                }

                SystemLogEntry[] newLogEntries = null;
                if (!_clientInfo._pendingLogEntries.isEmpty()) {
                    int entryCount = _clientInfo._pendingLogEntries.size();
                    newLogEntries = new SystemLogEntry[entryCount];
                    int ex = 0;
                    for (KomodoAppender.LogEntry localEntry : _clientInfo._pendingLogEntries) {
                        newLogEntries[ex] = new SystemLogEntry(localEntry._timeMillis, localEntry._source, localEntry._message);
                    }
                }

                ConsoleReadOnlyMessage[] newReadOnlyMessages = null;
                if (!_clientInfo._pendingReadOnlyMessages.isEmpty()) {
                    int msgCount = _clientInfo._pendingReadOnlyMessages.size();
                    newReadOnlyMessages = new ConsoleReadOnlyMessage[msgCount];
                    int mx = 0;
                    for (ReadOnlyMessage pendingMsg : _clientInfo._pendingReadOnlyMessages) {
                        newReadOnlyMessages[mx++] = new ConsoleReadOnlyMessage(pendingMsg._text);
                    }
                }

                if (_clientInfo._updatedHardwareConfiguration) {
                    //TODO
                }

                if (_clientInfo._updatedSystemConfiguration) {
                    //TODO
                }

                PollResult pollResult = new PollResult(_clientInfo._inputDelivered,
                                                       isMaster,
                                                       jumpKeyValue,
                                                       latestStatusMessage,
                                                       newLogEntries,
                                                       newReadOnlyMessages,
                                                       _clientInfo._updatedReadReplyMessages);

                respondWithJSON(_exchange, HttpURLConnection.HTTP_OK, pollResult);
                _clientInfo.clear();
            }
        }
    }

    /**
     * Handle posts to /session
     * Validates credentials and method
     * Creates and stashes a ClientInfo record for future method calls
     */
    private class APISessionRequestHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
            try {
                if (!validateCredentials(exchange)) {
                    respondUnauthorized(exchange);
                    return;
                }

                String method = exchange.getRequestMethod();
                if (!method.equalsIgnoreCase(HttpMethod.POST._value)) {
                    respondBadMethod(exchange);
                    return;
                }

                String clientId = UUID.randomUUID().toString();
                ClientInfo clientInfo = new ClientInfo(clientId);
                synchronized (_recentReadOnlyMessages) {
                    clientInfo._pendingReadOnlyMessages.addAll(_recentReadOnlyMessages);
                }

                clientInfo._remoteAddress = exchange.getRemoteAddress();
                clientInfo._inputDelivered = false;
                clientInfo._updatedJumpKeys = true;
                clientInfo._updatedStatusMessage = true;
                clientInfo._updatedHardwareConfiguration = true;
                clientInfo._updatedSystemConfiguration = true;
                synchronized (this) {
                    if (_clientInfos.isEmpty()) {
                        clientInfo._isMaster = true;
                        clientInfo._updatedReadReplyMessages = true;
                    }
                    _clientInfos.put(clientId, clientInfo);
                }

                respondWithJSON(exchange, HttpURLConnection.HTTP_CREATED, clientId);
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }
        }
    }

    /**
     * Handle all the web endpoint requests
     */
    private class WebHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            LOGGER.traceEntry(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));
            System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
            try {
                String fileName = exchange.getRequestURI().getPath();
                if (fileName.startsWith("/")) {
                    fileName = fileName.substring(1);
                }

                if (fileName.isEmpty() || fileName.equalsIgnoreCase("index.html")) {
                    fileName = HTML_FILE_NAME;
                }

                String mimeType = "";
                boolean textFile = false;
                if (fileName.endsWith(".html")) {
                    mimeType = "text/html";
                    textFile = true;
                } else if (fileName.endsWith(".css")) {
                    mimeType = "text/css";
                    textFile = true;
                } else if (fileName.endsWith(".js")) {
                    mimeType = "text/javascript";
                    textFile = true;
                } else if (fileName.endsWith(".json")) {
                    mimeType = "text/json";
                    textFile = true;
                } else {
                    mimeType = "application/octet-stream";
                }

                String fullName = String.format("%s/%s", _webRootPath, fileName);
                if (textFile) {
                    respondWithTextFile(exchange, HttpURLConnection.HTTP_OK, mimeType, fullName);
                } else {
                    respondWithBinaryFile(exchange, HttpURLConnection.HTTP_OK, mimeType, fullName);
                }
//                byte[] bytes = Files.readAllBytes(Paths.get(fileName));
//                String message = new String(bytes, StandardCharsets.US_ASCII);
//                exchange.getResponseHeaders().add("Content-type", "text/html");
//                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, message.length());
//                OutputStream os = exchange.getResponseBody();
//                os.write(message.getBytes());
//                os.close();
            } catch (IOException ex) {
                System.out.println(String.format("%s %s", ex.getClass().toString(), ex.getMessage()));//TODO remove
                respondWithText(exchange, HttpURLConnection.HTTP_NOT_FOUND, "Cannot find requested file");
            } catch (Throwable t) {
                LOGGER.catching(t);
            }
        }
    }

    /**
     * For debugging
     */
    public String getStackTrace(
        final Throwable t
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(t.toString());
        sb.append("\n");
        for (StackTraceElement e : t.getStackTrace()) {
            sb.append(e.toString());
            sb.append("\n");
        }
        return sb.toString();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  HTTP listener for the API
    //  All requests must use basic authentication for every message (in the headers, of course)
    //  All requests must include a (supposedly) unique UUID as a client identifier in the headers "Client={uuid}"
    //  This unique UUID must be used for every message sent by a given instance of a client.
    //  ----------------------------------------------------------------------------------------------------------------------------

    private class APIListener extends SecureServer {

        /**
         * constructor
         */
        private APIListener(
            final int portNumber
        ) {
            super("RESTSystemConsole", portNumber);
        }

        /**
         * Client wants us to start accepting requests
         */
        @Override
        public void setup(
        ) throws CertificateException,
                 InvalidKeyException,
                 IOException,
                 KeyManagementException,
                 KeyStoreException,
                 NoSuchAlgorithmException,
                 NoSuchProviderException,
                 SignatureException {
            super.setup();
            appendHandler("/", new APIDefaultRequestHandler());
            appendHandler("/jumpkeys", new APIJumpKeysRequestHandler());
            appendHandler("/message", new APIMessageHandler());
            appendHandler("/session", new APISessionRequestHandler());
            appendHandler("/poll", new APIPollRequestHandler());
            start();
        }

        /**
         * Owner wants us to stop accepting requests.
         * Tell our base class to STOP, then go wake up all the pending clients.
         */
        @Override
        public void stop() {
            super.stop();
            pokeClients(ClientInfo::clear);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  HTTP listener for the web service
    //  Since the user can do nothing without logging in through the API, there's little need to secure this.
    //  We'll Secure-HTTP it just for giggles, but there's no authentication going on.
    //  ----------------------------------------------------------------------------------------------------------------------------

    private class WebListener extends SecureServer {

        private WebListener(
            final int portNumber
        ) {
            super("WEBListener", portNumber);
        }

        @Override
        public void setup(
        ) throws CertificateException,
                 InvalidKeyException,
                 IOException,
                 KeyManagementException,
                 KeyStoreException,
                 NoSuchAlgorithmException,
                 NoSuchProviderException,
                 SignatureException {
            super.setup();
            appendHandler("/", new WebHandler());
            start();
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks the headers for a client id, then locates the corresponding ClientInfo object.
     * Returns null if ClientInfo object is not found or is unspecified.
     * Serves as validation for clients which have presumably previously done a POST to /session
     */
    private ClientInfo findClient(
        final HttpExchange exchange
    ) {
        List<String> values = exchange.getRequestHeaders().get("Client");
        if ((values != null) && (values.size() == 1)) {
            String clientId = values.get(0);
            synchronized (_clientInfos) {
                ClientInfo clientInfo = _clientInfos.get(clientId);
                if (clientInfo != null) {
                    return clientInfo;
                }
            }
        }

        return null;
    }

    /**
     * Thread-safe method for invoking a particular function on all established clients, then waking them up
     * @param pokeFunction A (potentially anonymous) class containing a function to be executed for eacah entity - if null, it is ignored
     */
    private void pokeClients(
        final PokeClientFunction pokeFunction
    ) {
        Set<ClientInfo> cinfos;
        synchronized (this) {
            cinfos = new HashSet<>(_clientInfos.values());
            _clientInfos.clear();
        }

        for (ClientInfo cinfo : cinfos) {
            synchronized (cinfo) {
                if (pokeFunction != null) {
                    pokeFunction.function(cinfo);
                }
                cinfo.notify();
            }
        }
    }

    /**
     * Client wants us to age-out any old client info objects
     */
    private void pruneClients() {
        synchronized (_clientInfos) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, ClientInfo>> iter = _clientInfos.entrySet().iterator();
            List<ClientInfo> removedClientInfos = new LinkedList<>();
            while (iter.hasNext()) {
                Map.Entry<String, ClientInfo> entry = iter.next();
                ClientInfo cinfo = entry.getValue();
                if (now > (cinfo._lastActivity + CLIENT_AGE_OUT_MSECS)) {
                    iter.remove();
                    removedClientInfos.add(cinfo);
                }
            }

            for (ClientInfo cinfo : removedClientInfos) {
                synchronized (cinfo) {
                    cinfo.clear();
                    cinfo.notify();
                }
            }
        }
    }

    /**
     * Convenient method for handling the situation where a method is requested which is not supported on the endpoint
     */
    private void respondBadMethod(
        final HttpExchange exchange
    ) {
        String response = String.format("Method %s is not supported for the given endpoint\n",
                                        exchange.getRequestMethod());
        respondWithText(exchange, HttpURLConnection.HTTP_BAD_METHOD, response);
    }

    /**
     * Convenient method for handling the situation where a particular request was in error.
     */
    private void respondBadRequest(
        final HttpExchange exchange,
        final String explanation
    ) {
        respondWithText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, explanation + "\n");
    }

    /**
     * Convenient method for handling the situation where no session exists
     */
    private void respondNoSession(
        final HttpExchange exchange
    ) {
        String response = "Forbidden - session not established\n";
        respondWithText(exchange, HttpURLConnection.HTTP_FORBIDDEN, response);
    }

    /**
     * Convenient method for handling the situation where we cannot find something which was requested by the client.
     */
    private void respondNotFound(
        final HttpExchange exchange,
        final String message
    ) {
        respondWithText(exchange, java.net.HttpURLConnection.HTTP_NOT_FOUND, message + "\n");
    }

    /**
     * Convenient method for handling an internal server error
     */
    private void respondServerError(
        final HttpExchange exchange,
        final String message
    ) {
        respondWithText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, message + "\n");
    }

    /**
     * Convenient method for setting up a 401 response
     */
    private void respondUnauthorized(
        final HttpExchange exchange
    ) {
        String response = "Unauthorized - credentials not provided or are invalid\nPlease enter credentials\n";
        respondWithText(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, response);
    }

    private void respondWithBinaryFile(
        final HttpExchange exchange,
        final int code,
        final String mimeType,
        final String fileName
    ) throws IOException {
        LOGGER.traceEntry(String.format("code:%d mimeType:%s fileName:%s", code, mimeType, fileName));
        byte[] bytes = Files.readAllBytes(Paths.get(fileName));
        exchange.getResponseHeaders().add("Content-type", mimeType);
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /**
     * When we need to send back a text fil
     * @param exchange HttpExchange we're dealing with
     * @param code response code - 200, 201, whatever
     * @param mimeType e.g., text/html
     * @param fileName Path/Filename of the file we need to send
     */
    private void respondWithTextFile(
        final HttpExchange exchange,
        final int code,
        final String mimeType,
        final String fileName
    ) throws IOException {
        LOGGER.traceEntry(String.format("code:%d mimeType:%s fileName:%s", code, mimeType, fileName));
        String message = new String(Files.readAllBytes(Paths.get(fileName)), "UTF-8");
        exchange.getResponseHeaders().add("Content-type", mimeType);
        byte[] bytes = message.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    /**
     * Convenient method for sending responses containing HTML text
     * @param exchange The HttpExchance object into which we inject the response
     * @param code The response code - 200, 201, 403, etc
     */
    private void respondWithHTML(
        final HttpExchange exchange,
        final int code,
        final String content
    ) {
        LOGGER.traceEntry(String.format("code:%d content:%s", code, content));
        System.out.println("-->" + content);   //TODO remove
        try {
            exchange.getResponseHeaders().add("Content-type", "text/html");
            exchange.sendResponseHeaders(code, content.length());
            OutputStream os = exchange.getResponseBody();
            os.write(content.getBytes());
            os.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Convenient method for sending responses containing JSON
     * @param exchange The HttpExchance object into which we inject the response
     * @param code The response code - 200, 201, 403, etc - most responses >= 300 won't necessarily have a JSON formatted body
     */
    private void respondWithJSON(
        final HttpExchange exchange,
        final int code,
        final Object object
    ) {
        LOGGER.traceEntry(String.format("code:%d object:%s", code, object.toString()));
        try {
            ObjectMapper mapper = new ObjectMapper();
            String content = mapper.writeValueAsString(object);
            System.out.println("-->" + content);   //TODO remove
            LOGGER.trace(String.format("  JSON:%s", content));
            exchange.getResponseHeaders().add("Content-type", "application/json");
            exchange.sendResponseHeaders(code, content.length());
            OutputStream os = exchange.getResponseBody();
            os.write(content.getBytes());
            os.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Convenient method for sending responses containing straight text
     * @param exchange The HttpExchance object into which we inject the response
     * @param code The response code - 200, 201, 403, etc
     */
    private void respondWithText(
        final HttpExchange exchange,
        final int code,
        final String content
    ) {
        LOGGER.traceEntry(String.format("code:%d content:%s", code, content));
        System.out.println("-->" + content);   //TODO remove
        try {
            exchange.getResponseHeaders().add("Content-type", "text/plain");
            exchange.sendResponseHeaders(code, content.length());
            OutputStream os = exchange.getResponseBody();
            os.write(content.getBytes());
            os.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Validate the credentials in the header of the given exchange object.
     * Only for POST to /session.
     * @return true if credentials are valid, else false
     */
    private boolean validateCredentials(
        final HttpExchange exchange
    ) {
        Headers headers = exchange.getRequestHeaders();
        List<String> values = headers.get("Authorization");
        if ((values != null) && (values.size() == 1)) {
            String[] split = values.get(0).split(" ");
            if (split.length == 2) {
                if (split[0].equalsIgnoreCase("Basic")) {
                    String unBased = new String(Base64.getDecoder().decode(split[1]));
                    String[] unBasedSplit = unBased.split(":");
                    if (unBasedSplit.length == 2) {
                        String givenUserName = unBasedSplit[0];
                        String givenClearTextPassword = unBasedSplit[1];
                        if (givenUserName.equalsIgnoreCase(_configurator._adminCredentials._userName)) {
                            return _configurator._adminCredentials.validatePassword(givenClearTextPassword);
                        }
                    }
                }
            }
        }

        return false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Implementations / overrides of abstract base methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For notifying clients that a pending ReadReplyMessage is no long pending,
     * at least insofar as the operating system is concerned.
     */
    public void cancelReadReplyMessage(
        final int messageId
    ) {
        synchronized (_pendingReadReplyMessages) {
            _pendingReadReplyMessages.remove(messageId);
        }

        pokeClients(clientInfo -> clientInfo._updatedReadReplyMessages = true);
    }

    /**
     * For debugging
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        try {
            writer.write(String.format("RESTSystemConsole %s\n", _name));
            writer.write(String.format("  WebListener commonName=%s portNumber=%d\n",
                                        _webListener.getCommonName(),
                                        _webListener.getPortNumber()));
            writer.write(String.format("  APIListener commonName=%s portNumber=%d\n",
                                       _apiListener.getCommonName(),
                                       _apiListener.getPortNumber()));

            writer.write("  Recent Read-Only Messages:\n");
            synchronized (_recentReadOnlyMessages) {
                for (ReadOnlyMessage msg : _recentReadOnlyMessages) {
                    writer.write(String.format("    '%s''\n", msg._text));
                }
            }

            writer.write("  Pending Read-Reply Messages:\n");
            synchronized (_pendingReadReplyMessages) {
                for (ReadReplyMessage msg : _pendingReadReplyMessages.values()) {
                    writer.write(String.format("    %d:'%s'", msg._messageId, msg._text));
                    writer.write(String.format("       Max reply:%d", msg._maxReplyLength));
                }
            }

            writer.write("  Pending input messages:\n");
            synchronized (_pendingInputMessages) {
                for (Map.Entry<String, InputMessage> entry : _pendingInputMessages.entrySet()) {
                    String clientId = entry.getKey();
                    InputMessage im = entry.getValue();
                    if (im instanceof UnsolicitedInputMessage) {
                        UnsolicitedInputMessage uim = (UnsolicitedInputMessage) im;
                        writer.write(String.format("    clientId=%s:'%s'\n", clientId, uim._text));
                    } else if (im instanceof ReadReplyInputMessage) {
                        ReadReplyInputMessage rrim = (ReadReplyInputMessage) im;
                        writer.write(String.format("    clientId=%s: %d '%s'\n", clientId, rrim._messageId, rrim._text));
                    }
                }
            }

            long now = System.currentTimeMillis();
            synchronized (_clientInfos) {
                for (ClientInfo cinfo : _clientInfos.values()) {
                    synchronized (cinfo) {
                        writer.write(String.format("  Client   %sRemote Address:%s   Last Activity %d msec ago\n",
                                                   cinfo._isMaster ? "MASTER " : "",
                                                   cinfo._remoteAddress.getAddress().getHostAddress(),
                                                   now - cinfo._lastActivity));

                        writer.write("  Pending output messages\n");
                        for (ReadOnlyMessage msg : cinfo._pendingReadOnlyMessages) {
                            writer.write(String.format("    '%s'\n", msg._text));
                        }
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    @Override
    public InputMessage pollInputMessage() {
        InputMessage result = null;
        synchronized (this) {
            Iterator<Map.Entry<String, InputMessage>> iter = _pendingInputMessages.entrySet().iterator();
            if (iter.hasNext()) {
                Map.Entry<String, InputMessage> firstEntry = iter.next();
                String clientId = firstEntry.getKey();
                result = firstEntry.getValue();
                ClientInfo cinfo = _clientInfos.get(clientId);
                if (cinfo != null) {
                    cinfo._inputDelivered = true;
                    synchronized (cinfo) {
                        cinfo.notify();
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void postReadOnlyMessage(
        final ReadOnlyMessage message
    ) {
        synchronized (_recentReadOnlyMessages) {
            _recentReadOnlyMessages.add(message);
            while (_recentReadOnlyMessages.size() > MAX_RECENT_READ_ONLY_MESSAGES) {
                _recentReadOnlyMessages.poll();
            }
        }

        pokeClients(clientInfo -> clientInfo._pendingReadOnlyMessages.add(message));
    }

    @Override
    public void postReadReplyMessage(
        final ReadReplyMessage message
    ) {
        synchronized (_pendingReadReplyMessages) {
            _pendingReadReplyMessages.put(message._messageId, message);
        }

        ReadOnlyMessage rom = new ReadOnlyMessage(message._text);
        synchronized (_clientInfos) {
            for (ClientInfo cinfo : _clientInfos.values()) {
                if (cinfo._isMaster) {
                    cinfo._updatedReadReplyMessages = true;
                } else {
                    cinfo._pendingReadOnlyMessages.add(rom);
                }
            }
        }

        pokeClients(null);
    }

    /**
     * Cache the given status message and notify the pending clients that an updated message is available
     */
    @Override
    public void postStatusMessage(
        final StatusMessage message
    ) {
        _latestStatusMessage = message;
        pokeClients(clientInfo -> clientInfo._updatedStatusMessage = true);
    }

    /**
     * Given a set of log entries, propagate all of the ones which do not come from black-listed sources, to any pending clients.
     * If there are none after filtering, don't annoy the clients.
     */
    @Override
    public void postSystemLogEntries(
        final KomodoAppender.LogEntry[] logEntries
    ) {
        List<KomodoAppender.LogEntry> logList = new LinkedList<>();
        for (KomodoAppender.LogEntry logEntry : logEntries) {
            boolean avoid = false;
            for (String s : _logReportingBlackList) {
                if (s.equalsIgnoreCase(logEntry._source)) {
                    avoid = true;
                    break;
                }
            }
            if (!avoid) {
                logList.add(logEntry);
            }
        }

        if (!logList.isEmpty()) {
            pokeClients(clientInfo -> clientInfo._pendingLogEntries.addAll(logList));
        }
    }

    /**
     * Reset all of the connected console sessions
     */
    @Override
    public void reset() {
        //  We don't close the established sessions, but we do clear out all messaging and such.
        pokeClients(ClientInfo::clear);
        synchronized(this) {
            _latestStatusMessage = null;
            _pendingInputMessages.clear();
            _pendingReadReplyMessages.clear();
            _recentReadOnlyMessages.clear();
        }
    }

    /**
     * Starts this entity
     */
    @Override
    public boolean start() {
        try {
            _webListener.setup();
            _apiListener.setup();
            _workerThread = new WorkerThread();
            _workerThread.start();
            return true;
        } catch (Exception ex) {
            LOGGER.catching(ex);
            return false;
        }
    }

    /**
     * Stops this entity
     */
    @Override
    public void stop() {
        _webListener.stop();
        _apiListener.stop();
        _workerThread._terminate = true;
        _workerThread = null;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Worker thread
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * This async thread/class exists mainly just to prune the established sessions periodically
     */
    private class WorkerThread extends Thread {

        public boolean _terminate = false;

        public void run() {
            while (!_terminate) {
                pruneClients();
                try {
                    Thread.sleep(WORKER_PERIODICITY_MSECS);
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }
        }
    }
}
