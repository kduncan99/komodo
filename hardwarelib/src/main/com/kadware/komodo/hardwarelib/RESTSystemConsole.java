/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.KomodoAppender;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.commlib.ConsoleInputMessage;
import com.kadware.komodo.commlib.ConsoleOutputMessage;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
        private long _lastActivity = System.currentTimeMillis();
        private InetSocketAddress _remoteAddress = null;

        public List<KomodoAppender.LogEntry> _pendingLogEntries = new LinkedList<>();
        public List<OutputMessage> _pendingOutputMessages = new LinkedList<>();
        public boolean _updatedHardwareConfiguration = false;
        public boolean _updatedJumpKeys = false;
        public boolean _updatedReadReplyMessages = false;
        public boolean _updatedStatusMessage = false;
        public boolean _updatedSystemConfiguration = false;

        public void clear() {
            _pendingLogEntries.clear();
            _pendingOutputMessages.clear();
            _updatedHardwareConfiguration = false;
            _updatedJumpKeys = false;
            _updatedReadReplyMessages = false;
            _updatedStatusMessage = false;
            _updatedSystemConfiguration = false;
        }

        public boolean hasUpdatesForClient() {
            return !_pendingLogEntries.isEmpty()
                || !_pendingOutputMessages.isEmpty()
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
    private static final String FAVICON_FILE_NAME = "../resources/systemConsole/favicon.jpg";
    private static final String HTML_FILE_NAME = "../resources/systemConsole/index.html";
    private static final int OUTPUT_CACHE_MAX_SIZE = 20;                    //  max 20 entries (unless we have >20 pinned)
    private static final long POLL_WAIT_MSECS = 10000;                      //  10 second (maximum) poll delay
    private static final long WORKER_PERIODICITY_MSECS = 10000;             //  worker thread does its work every 10 seconds

    private static final String[] _logReportingBlackList = { SystemProcessor.class.getName(),
                                                             RESTSystemConsole.class.getName() };

    private static final Logger LOGGER = LogManager.getLogger(RESTSystemConsole.class);

    private final Map<String, ClientInfo> _clientInfos = new HashMap<>();
    private final Configurator _configurator;
    private final JumpKeyPanel _jumpKeyPanel;
    private final APIListener _apiListener;
    private final WebListener _webListener;
    private final String _name;
    private Long _nextMessageIdentifier = 1L;
    private WorkerThread _workerThread = null;

    //  Recent output messages. Pinned messages are never allowed to scroll out of this list.
    //  This list is allowed to grow to a certain amount, but never beyond it unless required by the previous directive.
    private final Map<Long, OutputMessage> _outputMessageCache = new LinkedHashMap<>();

    private final List<InputMessage> _pendingInputMessages = new LinkedList<>();

    //  This is always the latest status message. Clients may pick it up at any time.
    private StatusMessage _latestStatusMessage = null;


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
        final JumpKeyPanel jumpKeyPanel
    ) {
        _name = name;
        _configurator = configurator;
        _jumpKeyPanel = jumpKeyPanel;
        _apiListener = new APIListener(2200);     //  TODO pull portnumber from Configurator hardware configuration
        _webListener = new WebListener(443);
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
                        content = new ObjectMapper().readValue(exchange.getRequestBody(), new TypeReference<JumpKeys>() {
                        });
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
            try {
                System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
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

                ObjectMapper mapper = new ObjectMapper();
                ConsoleInputMessage msg = mapper.readValue(exchange.getRequestBody(), ConsoleInputMessage.class);

                InputMessage im = new InputMessage(getNextMessageIdentifier(), msg._text);
                synchronized (_pendingInputMessages) {
                    _pendingInputMessages.add(im);
                }

                respondWithText(exchange, HttpURLConnection.HTTP_CREATED, "");
            } catch (IOException ex) {
                respondBadRequest(exchange, "Badly-formatted body");
            } catch (Throwable t) {
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

                SystemLogEntry[] newLogEntries = null;
                if (!_clientInfo._pendingLogEntries.isEmpty()) {
                    int entryCount = _clientInfo._pendingLogEntries.size();
                    newLogEntries = new SystemLogEntry[entryCount];
                    int ex = 0;
                    for (KomodoAppender.LogEntry localEntry : _clientInfo._pendingLogEntries) {
                        newLogEntries[ex] = new SystemLogEntry(localEntry._timeMillis, localEntry._source, localEntry._message);
                    }
                }

                ConsoleOutputMessage[] newOutputMessages = null;
                if (!_clientInfo._pendingOutputMessages.isEmpty()) {
                    int msgCount = _clientInfo._pendingOutputMessages.size();
                    newOutputMessages = new ConsoleOutputMessage[msgCount];
                    int mx = 0;
                    for (OutputMessage pendingMsg : _clientInfo._pendingOutputMessages) {
                        newOutputMessages[mx++] = new ConsoleOutputMessage(pendingMsg._identifier,
                                                                           pendingMsg._pinned,
                                                                           Arrays.copyOf(pendingMsg._text, pendingMsg._text.length));
                    }
                }

                if (_clientInfo._updatedHardwareConfiguration) {
                    //TODO
                }

                Long jumpKeyValue = null;
                if (_clientInfo._updatedJumpKeys) {
                    jumpKeyValue = _jumpKeyPanel.getJumpKeys().getW();
                }

                ConsoleStatusMessage latestStatusMessage = null;
                if (_clientInfo._updatedStatusMessage) {
                    latestStatusMessage = new ConsoleStatusMessage(_latestStatusMessage._identifier, _latestStatusMessage._text);
                }

                if (_clientInfo._updatedSystemConfiguration) {
                    //TODO
                }

                PollResult pollResult = new PollResult(jumpKeyValue, newLogEntries, latestStatusMessage, newOutputMessages);
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
                ClientInfo clientInfo = new ClientInfo();
                synchronized (_outputMessageCache) {
                    clientInfo._pendingOutputMessages.addAll(_outputMessageCache.values());
                }

                clientInfo._updatedJumpKeys = true;
                clientInfo._updatedReadReplyMessages = true;
                clientInfo._updatedStatusMessage = true;
                clientInfo._updatedHardwareConfiguration = true;
                clientInfo._updatedSystemConfiguration = true;
                clientInfo._remoteAddress = exchange.getRemoteAddress();
                synchronized (this) {
                    _clientInfos.put(clientId, clientInfo);
                }

                respondWithJSON(exchange, HttpURLConnection.HTTP_CREATED, clientId);
            } catch (Throwable t) {
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
            try {
            System.out.println(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));//TODO remove
            if (exchange.getRequestURI().getPath().equals("/favicon.ico")) {
                respondWithBinaryFile(exchange, HttpURLConnection.HTTP_OK, "image/jpeg", FAVICON_FILE_NAME);
            } else {
                byte[] bytes = Files.readAllBytes(Paths.get(HTML_FILE_NAME));
                String message = new String(bytes, StandardCharsets.US_ASCII);
                exchange.getResponseHeaders().add("Content-type", "text/html");
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, message.length());
                OutputStream os = exchange.getResponseBody();
                os.write(message.getBytes());
                os.close();
            }
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
    ) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(fileName));
            exchange.getResponseHeaders().add("Content-type", mimeType);
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
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
    ) {
        try {
            String message = new String(Files.readAllBytes(Paths.get(fileName)), "UTF-8");
            exchange.getResponseHeaders().add("Content-type", mimeType);
            byte[] bytes = message.getBytes();
            exchange.sendResponseHeaders(code, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
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
        try {
            System.out.println("-->" + content);   //TODO remove
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
        try {
            ObjectMapper mapper = new ObjectMapper();
            String content = mapper.writeValueAsString(object);
            System.out.println("-->" + content);   //TODO remove
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
        try {
            System.out.println("-->" + content);   //TODO remove
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

            writer.write("  Output Message Cache:\n");
            synchronized (_outputMessageCache) {
                for (OutputMessage msg : _outputMessageCache.values()) {
                    writer.write(String.format("    %d: pinned=%s\n", msg._identifier, msg._pinned));
                    for (String s : msg._text) {
                        writer.write(String.format("    ->'%s'\n", s));
                    }
                }
            }

            writer.write("  Pending input messages:\n");
            synchronized (_pendingInputMessages) {
                for (InputMessage msg : _pendingInputMessages) {
                    writer.write(String.format("    %d: '%s'\n", msg._identifier, msg._text));
                }
            }

            long now = System.currentTimeMillis();
            synchronized (_clientInfos) {
                for (ClientInfo cinfo : _clientInfos.values()) {
                    synchronized (cinfo) {
                        writer.write(String.format("  Client   Remote Address:%s   Last Activity %d msec ago\n",
                                                   cinfo._remoteAddress.getAddress().getHostAddress(),
                                                   now - cinfo._lastActivity));

                        writer.write("  Pending output messages\n");
                        for (OutputMessage msg : cinfo._pendingOutputMessages) {
                            writer.write(String.format("    %d: pinned=%s\n", msg._identifier, msg._pinned));
                            for (String s : msg._text) {
                                writer.write(String.format("    ->'%s'\n", s));
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            //  Do nothing
        }
    }

    /**
     * Used for sequencing / identifying message objects
     */
    @Override
    public long getNextMessageIdentifier() {
        synchronized (this) {
            return _nextMessageIdentifier++;
        }
    }

    @Override
    public InputMessage pollInputMessage() {
        synchronized (_pendingInputMessages) {
            if (!_pendingInputMessages.isEmpty()) {
                return _pendingInputMessages.remove(0);
            }
        }

        return null;
    }

    @Override
    public void postOutputMessage(
        final OutputMessage message
    ) {
        //  Add the message to our output cache. If it's new, yay.
        //  If a message with this identifier is still in our cache, we overlay it.
        //  Either way, we add it to the pending output for all the clients.
        synchronized (_outputMessageCache) {
            _outputMessageCache.put(message._identifier, message);
            Iterator<Map.Entry<Long, OutputMessage>> iter = _outputMessageCache.entrySet().iterator();
            while ((_outputMessageCache.size() > OUTPUT_CACHE_MAX_SIZE) && iter.hasNext()) {
                Map.Entry<Long, OutputMessage> entry = iter.next();
                if (!entry.getValue()._pinned) {
                    iter.remove();
                }
            }
        }

        pokeClients(clientInfo -> clientInfo._pendingOutputMessages.add(message));
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
        List<KomodoAppender.LogEntry> logList = new LinkedList<KomodoAppender.LogEntry>();
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
        synchronized (_outputMessageCache) {
            _outputMessageCache.clear();
        }

        _latestStatusMessage = null;
        pokeClients(ClientInfo::clear);
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
            //  Do nothing
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
                    //  Do nothing
                }
            }
        }
    }
}
