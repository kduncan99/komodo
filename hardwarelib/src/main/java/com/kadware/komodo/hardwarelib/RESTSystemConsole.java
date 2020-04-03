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
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
    private static final int MAX_LATEST_READ_ONLY_MESSAGES = 20;
    public static final int MAX_OUTSTANDING_READ_REPLY_MESSAGES = 10;
    private static final long POLL_WAIT_MSECS = 10000;                      //  10 second (maximum) poll delay
    private static final long WORKER_PERIODICITY_MSECS = 1000;              //  worker thread does its work every 1 second

    private static final Logger LOGGER = LogManager.getLogger(RESTSystemConsole.class);

    private Map<String, ClientInfo> _clientInfos = new HashMap<>();
    private final Configurator _configurator;
    private final JumpKeyPanel _jumpKeyPanel;
    private final Listener _listener;
    private final String _name;
    private Long _nextMessageIdentifier = 1L;
    private WorkerThread _workerThread = null;

    //  Recent output messages. Pinned messages are never allowed to scroll out of this list.
    //  This list is allowed to grow to a certain amount, but never beyond it unless required by the previous directive.
    private final Map<Long, OutputMessage> _outputMessageCache = new HashMap<>();

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
        _listener = new Listener(2200);     //  TODO pull this from Configurator hardware configuration eventually
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Endpoint handlers, to be attached to the HTTP listener
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Invalid path handler class
     */
    private class DefaultRequestHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            if (!validateCredentials(exchange)) {
                respondUnauthorized(exchange);
                return;
            }

            ClientInfo clientInfo = findClient(exchange);
            if (clientInfo == null) {
                respondNoSession(exchange);
                return;
            }

            clientInfo._lastActivity = System.currentTimeMillis();
            respondNotFound(exchange, "Path or object does not exist");
        }
    }

    /**
     * Handles requests against the /jumpkeys path
     * GET retrieves the current settings as a WORD36 wrapped in a long.
     * PUT accepts the JK settings as a WORD36 wrapped in a long, and persists them to the singular system jump key panel.
     * Either way, JK36 is in the least-significant bit and JKn is 36-n bits to the left of the LSB.
     */
    private class JumpKeysRequestHandler implements HttpHandler {

        private JumpKeys createJumpKeys(
            final long compositeValue
        ) {
            JumpKeys jumpKeysResponse = new JumpKeys();
            jumpKeysResponse._compositeValue = compositeValue;
            long bitMask = 0_400000_000000L;
            for (int jkid = 1; jkid <= 36; jkid++) {
                jumpKeysResponse._componentValues.put(jkid, (jumpKeysResponse._compositeValue & bitMask) != 0);
                bitMask >>= 1;
            }
            return jumpKeysResponse;
        }

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            if (!validateCredentials(exchange)) {
                respondUnauthorized(exchange);
                return;
            }

            ClientInfo clientInfo = findClient(exchange);
            if (clientInfo == null) {
                respondNoSession(exchange);
                return;
            }

            clientInfo._lastActivity = System.currentTimeMillis();

            //  For GET - return the settings as both a composite value and a map of individual jump key settings
            if (exchange.getRequestMethod().equalsIgnoreCase(HttpMethod.GET._value)) {
                JumpKeys jumpKeysResponse = createJumpKeys(_jumpKeyPanel.getJumpKeys().getW());
                respondNormal(exchange, jumpKeysResponse);
                return;
            }

            //  For PUT - accept the input object - if it has a composite value, use that to set the entire jump key panel.
            //  If it has no composite value, but it has component values, use them to individually set the jump keys.
            //  If it has neither, reject the PUT.
            if (exchange.getRequestMethod().equalsIgnoreCase(HttpMethod.PUT._value)) {
                JumpKeys content;
                try {
                    content = new ObjectMapper().readValue(exchange.getRequestBody(), new TypeReference<JumpKeys>() {});
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
                    JumpKeys response = createJumpKeys(content._compositeValue);
                    respondNormal(exchange, response);
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
                        respondNormal(exchange, jumpKeysResponse);
                        return;
                    }
                }

                respondBadRequest(exchange, "Requires either composite or component values");
                return;
            }

            //  Neither a GET or a PUT - this is not allowed.
            respondBadMethod(exchange);
        }
    }

    /**
     * Provides a method for injecting input to the system via POST to /message
     */
    private class MessageHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            if (!validateCredentials(exchange)) {
                respondUnauthorized(exchange);
                return;
            }

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

            //  TODO implement the rest of the code
        }
    }

    /**
     * Handle a poll request (a GET to /poll).
     * Check to see if there is anything new.  If so, send it.
     * Otherwise, wait for some period of time to see whether anything new pops up.
     */
    private class PollRequestHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            if (!validateCredentials(exchange)) {
                respondUnauthorized(exchange);
                return;
            }

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

            new PollThread(exchange, clientInfo).start();
        }
    }

    /**
     * One of these is spun off for every GET on /poll. We delay returning until either we reach a timeout in which
     * case we return an empty poll result, or there is an update which our particular client would like to know about.
     * Needed so we can go back and service other requests while we're waiting on anything noteworthy to occur.
     */
    class PollThread extends Thread {

        private final ClientInfo _clientInfo;
        private final HttpExchange _exchange;

        private PollThread(
            final HttpExchange exchange,
            final ClientInfo clientInfo
        ) {
            _clientInfo = clientInfo;
            _exchange = exchange;
        }

        @Override
        public void run() {
            //  Update last poll time, then check if there are any updates already waiting for the client to pick up.
            //  If not, go into a wait loop, which will be interrupted if any updates eventuate during the wait.
            //  At the end of the wait construct and return a SystemProcessorPollResult object
            _clientInfo._lastActivity = System.currentTimeMillis();
            synchronized (_clientInfo) {
                if (!_clientInfo.hasUpdatesForClient()) {
                    try {
                        _clientInfo.wait(POLL_WAIT_MSECS);
                    } catch (InterruptedException ex) {
                        LOGGER.catching(ex);
                    }
                }
            }

            PollResult pollResult = new PollResult();

            if (!_clientInfo._pendingLogEntries.isEmpty()) {
                int entryCount = _clientInfo._pendingLogEntries.size();
                pollResult._newLogEntries = new SystemLogEntry[entryCount];
                int ex = 0;
                for (KomodoAppender.LogEntry localEntry : _clientInfo._pendingLogEntries) {
                    SystemLogEntry logEntry = new SystemLogEntry();
                    logEntry._timestamp = localEntry._timeMillis;
                    logEntry._entity = localEntry._source;
                    logEntry._message = localEntry._message;
                    pollResult._newLogEntries[ex] = logEntry;
                }
            }

            if (!_clientInfo._pendingOutputMessages.isEmpty()) {
                int msgCount = _clientInfo._pendingOutputMessages.size();
                pollResult._newOutputMessages = new ConsoleOutputMessage[msgCount];
                int mx = 0;
                for (OutputMessage pendingMsg : _clientInfo._pendingOutputMessages) {
                    ConsoleOutputMessage commMsg = new ConsoleOutputMessage();
                    commMsg._identifier = pendingMsg._identifier;
                    commMsg._pinned = pendingMsg._pinned;
                    commMsg._text = Arrays.copyOf(pendingMsg._text, pendingMsg._text.length);
                    pollResult._newOutputMessages[mx++] = commMsg;
                }
                _clientInfo._pendingOutputMessages.clear();
            }

            if (_clientInfo._updatedHardwareConfiguration) {
                //TODO
            }

            if (_clientInfo._updatedJumpKeys) {
                pollResult._jumpKeySettings = _jumpKeyPanel.getJumpKeys().getW();
            }

            if (_clientInfo._updatedStatusMessage) {
                pollResult._latestStatusMessage = new ConsoleStatusMessage();
                pollResult._latestStatusMessage._text = Arrays.copyOf(_latestStatusMessage._text, _latestStatusMessage._text.length);
            }

            if (_clientInfo._updatedSystemConfiguration) {
                //TODO
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                String response = mapper.writeValueAsString(pollResult);
                _exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length());
                OutputStream os = _exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } catch (IOException ex) {
                LOGGER.catching(ex);
            }
        }
    }

    /**
     * Handle posts to /session
     * Validates credentials and method
     * Creates and stashes a ClientInfo record for future method calls
     */
    private class SessionRequestHandler implements HttpHandler {

        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
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
            clientInfo._pendingOutputMessages.addAll(_outputMessageCache.values()); //  TODO probably not thread-safe, what to do?
            clientInfo._updatedJumpKeys = true;
            clientInfo._updatedReadReplyMessages = true;
            clientInfo._updatedStatusMessage = true;
            clientInfo._updatedHardwareConfiguration = true;
            clientInfo._updatedSystemConfiguration = true;
            clientInfo._remoteAddress = exchange.getRemoteAddress();
            synchronized (this) {
                _clientInfos.put(clientId, clientInfo);
            }

            respondCreated(exchange, clientId);
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  HTTP listener
    //  All requests must use basic authentication for every message (in the headers, of course)
    //  All requests must include a (supposedly) unique UUID as a client identifier in the headers "Client={uuid}"
    //  This unique UUID must be used for every message sent by a given instance of a client.
    //  ----------------------------------------------------------------------------------------------------------------------------

    private class Listener extends SecureServer {

        /**
         * constructor
         */
        private Listener(
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
            appendHandler("/", new DefaultRequestHandler());
            appendHandler("/jumpkeys", new JumpKeysRequestHandler());
            appendHandler("/message", new MessageHandler());
            appendHandler("/session", new SessionRequestHandler());
            appendHandler("/poll", new PollRequestHandler());
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
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks the headers for a client id, then locates the corresponding ClientInfo object.
     * Returns null if ClientInfo object is not found or unspecified.
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
     * Convenient method for sending responses of any particular kind
     */
    private void respond(
        final HttpExchange exchange,
        final int code,
        final String text
    ) {
        try {
            exchange.sendResponseHeaders(code, text.length());
            OutputStream os = exchange.getResponseBody();
            os.write(text.getBytes());
            os.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
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
        respond(exchange, HttpURLConnection.HTTP_BAD_METHOD, response);
    }

    /**
     * Convenient method for handling the situation where a particular request was in error.
     */
    private void respondBadRequest(
        final HttpExchange exchange,
        final String explanation
    ) {
        String response = String.format("%s\n", explanation);
        respond(exchange, HttpURLConnection.HTTP_BAD_REQUEST, response);
    }

    /**
     * Convenient method for JSON-ifying a particular response entity, and sending it back with a 201
     */
    private void respondCreated(
        final HttpExchange exchange,
        final Object jsonMappableObject
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String response = mapper.writeValueAsString(jsonMappableObject);
            respond(exchange, HttpURLConnection.HTTP_CREATED, response);
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Convenient method for JSON-ifying a particular response entity, and sending it back with a 200
     */
    private void respondNormal(
        final HttpExchange exchange,
        final Object jsonMappableObject
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String response = mapper.writeValueAsString(jsonMappableObject);
            respond(exchange, HttpURLConnection.HTTP_OK, response);
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Convenient method for handling the situation where no session exists
     */
    private void respondNoSession(
        final HttpExchange exchange
    ) {
        String response = "Forbidden - session not established\n";
        respond(exchange, HttpURLConnection.HTTP_FORBIDDEN, response);
    }

    /**
     * Convenient method for handling the situation where we cannot find something which was requested by the client.
     */
    private void respondNotFound(
        final HttpExchange exchange,
        final String message
    ) {
        String response = String.format("%s\n", message);
        respond(exchange, java.net.HttpURLConnection.HTTP_NOT_FOUND, response);
    }

    /**
     * Convenient method for setting up a 401 response
     */
    private void respondUnauthorized(
        final HttpExchange exchange
    ) {
        String response = "Unauthorized - credentials not provided or are invalid\nPlease enter credentials\n";
        respond(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, response);
    }

    /**
     * Validate the credentials in the header of the given exchange object.
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
                String givenUserName = split[0];
                if (givenUserName.equalsIgnoreCase(_configurator._adminCredentials._userName)) {
                    String givenClearTextPassword = split[1];
                    return _configurator._adminCredentials.validatePassword(givenClearTextPassword);
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
            Set<ClientInfo> cinfos;
            synchronized (this) {
                cinfos = new java.util.HashSet<>(_clientInfos.values());
                writer.write(String.format("RESTSystemConsole %s\n", _name));
                writer.write(String.format(
                    "  Listener commonName=%s portNumber=%d\n",
                    _listener.getCommonName(),
                    _listener.getPortNumber()));

                //TODO show any cached messages
            }

            long now = System.currentTimeMillis();
            for (ClientInfo cinfo : cinfos) {
                synchronized (cinfo) {
                    writer.write(String.format("  Client   Remote Address:%s   Last Activity %d msec ago\n",
                                               cinfo._remoteAddress.getAddress().getHostAddress(),
                                               now - cinfo._lastActivity));
                    //  TODO show pending stuff
                }
            }
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Used for sequencing / identifying message objects
     */
    @Override
    public long getNextMessageIdentifier() {
        synchronized (_nextMessageIdentifier) {
            return _nextMessageIdentifier++;
        }
    }

    @Override
    public InputMessage pollInputMessage() {
        //TODO
        return null;
    }

    @Override
    public void postOutputMessage(
        final OutputMessage message
    ) {
        //  Add the message to our output cache. If it's new, yay.
        //  If a message with this identifier is still in our cache, we overlay it.
        //  Either way, we add it to the pending output for all the clients.
        synchronized (this) {
            _outputMessageCache.put(message._identifier, message);
        }

        pokeClients(clientInfo -> clientInfo._pendingOutputMessages.add(message));
    }

    @Override
    public void postStatusMessage(
        final StatusMessage message
    ) {
        synchronized (this) {
            _latestStatusMessage = message;
        }

        pokeClients(clientInfo -> clientInfo._updatedStatusMessage = true);
    }

    @Override
    public void postSystemLogEntries(
        final KomodoAppender.LogEntry[] logEntries
    ) {
        List<KomodoAppender.LogEntry> logList = Arrays.asList(logEntries);
        pokeClients(clientInfo -> clientInfo._pendingLogEntries.addAll(logList));
    }

    @Override
    public void reset() {
        //  We don't close the established sessions, but we do clear out all messaging and such.
        synchronized (this) {
            _outputMessageCache.clear();
            _latestStatusMessage = null;
            pokeClients(ClientInfo::clear);
        }
    }

    @Override
    public boolean start() {
        try {
            _listener.setup();
            _workerThread = new WorkerThread();
            _workerThread.start();
            return true;
        } catch (Exception ex) {
            LOGGER.catching(ex);
            return false;
        }
    }

    @Override
    public void stop() {
        _workerThread._terminate = true;
        _workerThread = null;
        _listener.stop();
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
