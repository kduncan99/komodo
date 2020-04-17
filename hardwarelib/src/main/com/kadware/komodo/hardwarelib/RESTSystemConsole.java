/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.KomodoLoggingAppender;
import com.kadware.komodo.baselib.PathNames;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.HttpMethod;
import com.kadware.komodo.baselib.SecureServer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
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
 *
 * This implementation uses long polling.
 * Keeping it simple - we require clients to implement a 24x80 output screen, with a separate input text facility.
 * This can change by altering the screen size constants here; the client should be implemented such that it can
 * easily be changed as well.
 */
@SuppressWarnings("Duplicates")
public class RESTSystemConsole implements SystemConsole {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  interface for anonymous-class-based client notification
    //  ----------------------------------------------------------------------------------------------------------------------------

    private interface PokeClientFunction {
        void function(final SessionInfo sessionInfo);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Communications packets sent between us and the REST client
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Information sent to us by the client when it attempts to establish a session
     */
    private static class ClientAttributes {
        @JsonProperty("screenSizeColumns")  final int _screenSizeColumns;
        @JsonProperty("screenSizeRows")    final int _screenSizeRows;

        ClientAttributes(
            @JsonProperty("screenSizeColumns")  final int screenSizeColumns,
            @JsonProperty("screenSizeRows")     final int screenSizeRows
        ) {
            _screenSizeColumns = screenSizeColumns;
            _screenSizeRows = screenSizeRows;
        }
    }

    /**
     * Client sends this to us whenever the operator hits 'enter'
     */
    private static class InputMessage {
        @JsonProperty("text")           final String _text;

        InputMessage(
            @JsonProperty("text")       final String text
        ) {
            _text = text;
        }
    }

    /**
     * Object describing or requesting a change in the current jump key settings for a SystemProcessor.
     */
    private static class JumpKeys {
        //  36-bit composite value, wrapped in a long.
        @JsonProperty("compositeValue") public Long _compositeValue;

        //  Individual values, per bit.
        //  Key is the jump key identifier from 1 to 36
        //  Value is true if jk is set (or to be set), false if jk is clear (or to be clear).
        //  For PUT an unspecified jk means the value is left as-is.
        //  For GET values for all jump keys are returned.
        @JsonProperty("componentValues") public Map<Integer, Boolean> _componentValues;

        JumpKeys(
            @JsonProperty("compositeValue")     final Long compositeValue,
            @JsonProperty("componentValues")    final Map<Integer, Boolean> componentValues
        ) {
            _compositeValue = compositeValue;
            _componentValues = new HashMap<>(componentValues);
        }
    }

    /**
     * We send these to the client to affect what is on its display.
     */
    private static class OutputMessage {

        static final int OMTYPE_CLEAR_SCREEN = 0;
        static final int OMTYPE_UNLOCK_KEYBOARD = 1;
        static final int OMTYPE_DELETE_ROW = 2;
        static final int OMTYPE_WRITE_ROW = 3;

        @JsonProperty("backgroundColor")        final Integer _backgroundColor; //  0xrrggbb format
        @JsonProperty("messageType")            final Integer _messageType;
        @JsonProperty("rowIndex")               final Integer _rowIndex;
        @JsonProperty("textColor")              final Integer _textColor;       //  0xrrggbb format
        @JsonProperty("text")                   final String _text;
        @JsonProperty("rightJustified")         final Boolean _rightJustified;

        OutputMessage(
            @JsonProperty("messageType")        final Integer messageType,      //  all msgs
            @JsonProperty("rowIndex")           final Integer rowIndex,         //  DELETE_ROW, WRITE_ROW
            @JsonProperty("textColor")          final Integer textColor,        //  WRITE_ROW
            @JsonProperty("backgroundColor")    final Integer backgroundColor,  //  WRITE_ROW
            @JsonProperty("text")               final String text,              //  WRITE_ROW
            @JsonProperty("rightJustified")     final Boolean rightJustified    //  WRITE_ROW
        ) {
            _messageType = messageType;
            _rowIndex = rowIndex;
            _textColor = textColor;
            _backgroundColor = backgroundColor;
            _text = text;
            _rightJustified = rightJustified;
        }

        static OutputMessage createClearScreenMessage() {
            return new OutputMessage(OMTYPE_CLEAR_SCREEN, null, null, null, null, null);
        }

        static OutputMessage createUnlockKeyboardMessage() {
            return new OutputMessage(OMTYPE_UNLOCK_KEYBOARD, null, null, null, null, null);
        }

        static OutputMessage createDeleteRowMessage(
            final int rowIndex
        ) {
            return new OutputMessage(OMTYPE_DELETE_ROW, rowIndex, null, null, null, null);
        }

        static OutputMessage createWriteRowMessage(
            final int rowIndex,
            final int textColor,
            final int backgroundColor,
            final String text,
            final Boolean rightJustified
        ) {
            return new OutputMessage(OMTYPE_WRITE_ROW, rowIndex, textColor, backgroundColor, text, rightJustified);
        }
    }

    /**
     * Object encapsulating certain other objects.
     * Client issues a GET on the /poll subdirectory, and we respond with all the updated information.
     * An entity will be null if that entity has not been updated in the interim.
     */
    private static class PollResult {

        //  populated by us when the JK settings have changed
        @JsonProperty("jumpKeySettings")            public Long _jumpKeySettings;

        //  Populated by us when any new log entries are available
        @JsonProperty("newLogEntries")              public SystemLogEntry[] _logEntries;

        //  Output messages we send to the client.
        @JsonProperty("outputMessages")             public OutputMessage[] _outputMessages;

        PollResult() {
            _jumpKeySettings = null;
            _logEntries = new SystemLogEntry[0];
            _outputMessages = new OutputMessage[0];
        }

        PollResult(
            @JsonProperty("jumpKeySettings")            final Long jumpKeySettings,
            @JsonProperty("newLogEntries")              final SystemLogEntry[] logEntries,
            @JsonProperty("outputMessages")             final OutputMessage[] outputMessages
        ) {
            _jumpKeySettings = jumpKeySettings;
            _logEntries = logEntries == null ? null : Arrays.copyOf(logEntries, logEntries.length);
            _outputMessages = outputMessages == null ? null : Arrays.copyOf(outputMessages, outputMessages.length);
        }
    }

    /**
     * Object describing a log entry we've caught from the system logger, to be sent on to the client
     */
    private static class SystemLogEntry {
        @JsonProperty("timestamp")              public Long _timestamp;     //  system milliseconds
        @JsonProperty("category")               public String _category;    //  ERROR, TRACE, etc
        @JsonProperty("entity")                 public String _entity;      //  reporting entity
        @JsonProperty("message")                public String _message;

        SystemLogEntry (
            @JsonProperty("timestamp")      final Long timestamp,
            @JsonProperty("category")       final String category,
            @JsonProperty("entity")         final String entity,
            @JsonProperty("message")        final String message
        ) {
            _timestamp = timestamp;
            _category = category;
            _entity = entity;
            _message = message;
        }

        SystemLogEntry(
            KomodoLoggingAppender.LogEntry logEntry
        ) {
            _category = logEntry._category;
            _entity = logEntry._source;
            _message = logEntry._message;
            _timestamp = logEntry._timeMillis;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Internal classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static class PendingReadReplyMessage {
        int _currentRowIndex;
        final int _maxReplyLength;
        final int _messageId;
        final String _text;

        PendingReadReplyMessage(
            final int rowIndex,
            final int maxReplyLength,
            final int messageId,
            final String text
        ) {
            _currentRowIndex = rowIndex;
            _maxReplyLength = maxReplyLength;
            _messageId = messageId;
            _text = text;
        }
    }

    /**
     * SessionIInfo - information regarding a particular client
     */
    private static class SessionInfo {

        private final ClientAttributes _clientAttributes;
        private final String _clientId;
        private long _lastActivity = System.currentTimeMillis();
        private boolean _isMaster = false;
        private PollResult _pollResult = new PollResult();
        private InetSocketAddress _remoteAddress = null;
        private int _statusMessageCount = 0;

        //  Read Reply messages which have not yet been responded to
        //  Key is the messageId, value is a PendingReadReplyMessage object
        private final Map<Integer, PendingReadReplyMessage> _pendingReadReplyMessages = new HashMap<>();

        //  A row is 'pinned' if it contains content which cannot be allowed to scroll off the display.
        //  Examples are status messages and pending read reply messages.
        //  This table is indexed by the corresponding row index (which is the row number minus 1, if anyone cares).
        private final boolean[] _pinnedState;

        SessionInfo(
            final String clientId,
            final ClientAttributes clientAttributes
        ) {
            _clientAttributes = clientAttributes;
            _clientId = clientId;
            _pinnedState = new boolean[clientAttributes._screenSizeRows];
        }

        void appendLogEntries(
            final List<SystemLogEntry> entries
        ) {
            PollResult pr = _pollResult;
            if (pr._logEntries == null) {
                pr._logEntries = entries.toArray(new SystemLogEntry[0]);
            } else {
                int origArrayLen = pr._logEntries.length;
                int newArrayLen = origArrayLen + entries.size();
                SystemLogEntry[] newArray = Arrays.copyOf(pr._logEntries, newArrayLen);
                int newx = origArrayLen;
                for (SystemLogEntry sle : entries) {
                    newArray[newx++] = sle;
                }
                pr._logEntries = newArray;
            }
        }

        void appendOutputMessage(
            final OutputMessage newMessage
        ) {
            PollResult pr = _pollResult;
            if (pr._outputMessages == null) {
                pr._outputMessages = new OutputMessage[1];
                pr._outputMessages[0] = newMessage;
            } else {
                int origLen = pr._outputMessages.length;
                pr._outputMessages = Arrays.copyOf(pr._outputMessages, origLen + 1);
                pr._outputMessages[origLen] = newMessage;
            }
        }

        void appendOutputMessages(
            final List<OutputMessage> entries
        ) {
            PollResult pr = _pollResult;
            if (pr._outputMessages == null) {
                pr._outputMessages = entries.toArray(new OutputMessage[0]);
            } else {
                int origArrayLen = pr._outputMessages.length;
                int newArrayLen = origArrayLen + entries.size();
                OutputMessage[] newArray = Arrays.copyOf(pr._outputMessages, newArrayLen);
                int newx = origArrayLen;
                for (OutputMessage msg : entries) {
                    newArray[newx++] = msg;
                }
                pr._outputMessages = newArray;
            }
        }

        /**
         * Cancels a read-reply message - generally means that the message has been responded to
         */
        void cancelReadReplyMessage(
            final int messageId
        ) {
            PendingReadReplyMessage prrm = _pendingReadReplyMessages.get(messageId);
            if (prrm != null) {
                String text = ("  " + prrm._text).substring(0, _clientAttributes._screenSizeColumns);
                appendOutputMessage(OutputMessage.createWriteRowMessage(prrm._currentRowIndex,
                                                                        READ_ONLY_FG_COLOR,
                                                                        READ_ONLY_BG_COLOR,
                                                                        text,
                                                                        false));
                _pendingReadReplyMessages.remove(messageId);
            }
        }

        /**
         * Indicates whether we have information to send back to the client.
         */
        boolean hasUpdates() {
            PollResult pr = _pollResult;
            return (pr._jumpKeySettings != null)
                || (pr._logEntries.length > 0)
                || (pr._outputMessages.length > 0);
        }

        /**
         * Resets the client
         */
        void resetClient() {
            _pendingReadReplyMessages.clear();
            List<OutputMessage> newMessages = new LinkedList<>();
            newMessages.add(OutputMessage.createClearScreenMessage());
            newMessages.add(OutputMessage.createUnlockKeyboardMessage());
            appendOutputMessages(newMessages);
        }

        /**
         * Posts an echo of an input message
         */
        void postInputMessage(
            final String message
        ) {
            scroll();
            int rx = _clientAttributes._screenSizeRows - 1;
            appendOutputMessage(OutputMessage.createWriteRowMessage(rx, INPUT_FG_COLOR, INPUT_BG_COLOR, message, false));
        }

        /**
         * Posts a read-only message to this client
         */
        void postReadOnlyMessage(
            final String message,
            final Boolean rightJustified
        ) {
            scroll();
            int rx = _clientAttributes._screenSizeRows - 1;
            appendOutputMessage(OutputMessage.createWriteRowMessage(rx,
                                                                    READ_ONLY_FG_COLOR,
                                                                    READ_ONLY_BG_COLOR,
                                                                    message,
                                                                    rightJustified));
        }

        /**
         * Posts a new read-reply message to the client
         */
        void postReadReplyMessage(
            final int messageId,
            final String message,
            final int maxReplySize
        ) {
            if ((messageId >= 0) && (messageId <= 9) && !_pendingReadReplyMessages.containsKey(messageId)) {
                scroll();
                String text = String.format("%d %s", messageId, message);
                int rx = _clientAttributes._screenSizeRows - 1;
                appendOutputMessage(OutputMessage.createWriteRowMessage(rx,
                                                                        READ_REPLY_FG_COLOR,
                                                                        READ_REPLY_BG_COLOR,
                                                                        text,
                                                                        false));

                PendingReadReplyMessage prrm = new PendingReadReplyMessage(rx, maxReplySize, messageId, message);
                _pendingReadReplyMessages.put(messageId, prrm);
            }
        }

        /**
         * Posts a new set of status messages to the client.
         * Note that we have to ensure that we do not accidentally overwrite any pending read-reply messages.
         * If the new set of messages has more content than the previous set, and if we would overwrite one or more
         *      pending read-reply messages, then those messages have to be moved to the bottom of the screen - i.e., they
         *      must be effectively resent.
         * If the new set has less content, we need to overwrite the previous excessive status message rows with blank rows.
         */
        void postStatusMessages(
            final String[] messages
        ) {
            List<OutputMessage> output = new LinkedList<>();
            for (int mx = 0; mx < _statusMessageCount; ++mx) {
                if (mx < messages.length) {
                    output.add(OutputMessage.createWriteRowMessage(mx, STATUS_FG_COLOR, STATUS_BG_COLOR, messages[mx], false));
                    _pinnedState[mx] = true;
                } else {
                    output.add(OutputMessage.createWriteRowMessage(mx, READ_ONLY_FG_COLOR, READ_ONLY_BG_COLOR, "",false));
                    _pinnedState[mx] = false;
                }
            }
            appendOutputMessages(output);

            List<PendingReadReplyMessage> displaced = new LinkedList<>();
            Iterator<Map.Entry<Integer, PendingReadReplyMessage>> iter = _pendingReadReplyMessages.entrySet().iterator();
            while (iter.hasNext()) {
                PendingReadReplyMessage prrm = iter.next().getValue();
                if (prrm._currentRowIndex < messages.length) {
                    displaced.add(prrm);
                    iter.remove();
                }
            }

            int lastRowIndex = _clientAttributes._screenSizeRows - 1;
            for (PendingReadReplyMessage prrm : displaced) {
                scroll();
                prrm._currentRowIndex = lastRowIndex;
                _pendingReadReplyMessages.put(prrm._messageId, prrm);
                _pinnedState[lastRowIndex] = true;
            }

            _statusMessageCount = messages.length;
        }

        /**
         * Finds the first delete-able row on the client's display, and effectively deletes it,
         * scrolling all rows below it up by one row.
         */
        void scroll() {
            //  Find the index of the first non-pinned row
            int rx = 0;
            while (_pinnedState[rx]) {
                rx++;
            }

            //  Scroll - create client output to implement this visually, then scroll all our internal objects as well.
            List<OutputMessage> output = new LinkedList<>();
            output.add(OutputMessage.createDeleteRowMessage(rx));
            output.add(OutputMessage.createWriteRowMessage(_clientAttributes._screenSizeRows - 1,
                                                           READ_ONLY_FG_COLOR,
                                                           READ_ONLY_BG_COLOR,
                                                           "",
                                                           false));

            for (PendingReadReplyMessage prrm : _pendingReadReplyMessages.values()) {
                if (prrm._currentRowIndex > rx) {
                    prrm._currentRowIndex--;
                }
            }
            for (int ry = rx + 1; ry < _clientAttributes._screenSizeRows; ++ry) {
                _pinnedState[ry - 1] = _pinnedState[ry];
            }

            appendOutputMessages(output);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final int INPUT_FG_COLOR = 0xffffff;
    private static final int INPUT_BG_COLOR = 0x000000;
    private static final int READ_ONLY_FG_COLOR = 0x00ff00;
    private static final int READ_ONLY_BG_COLOR = 0x000000;
    private static final int READ_REPLY_FG_COLOR = 0xffff00;
    private static final int READ_REPLY_BG_COLOR = 0x000000;
    private static final int STATUS_FG_COLOR = 0x00ffff;
    private static final int STATUS_BG_COLOR = 0x000000;

    private static final long CLIENT_AGE_OUT_MSECS = 10 * 60 * 1000;        //  10 minutes of no polling ages out a client
    private static final String HTML_FILE_NAME = "systemConsole/systemConsole.html";
    private static final String FAVICON_FILE_NAME = "systemConsole/favicon.png";
    private static final long POLL_WAIT_MSECS = 10000;                      //  10 second (maximum) poll delay
    private static final int MAX_CACHED_LOG_ENTRIES = 200;                  //  max size of most-recent log entries
    private static final int MAX_CACHED_READ_ONLY_MESSAGES = 30;            //  max size of container of most-recent RO messages
    private static final int MAX_CONSOLE_SIZE_COLUMNS = 132;
    private static final int MAX_CONSOLE_SIZE_ROWS = 50;
    private static final int MIN_CONSOLE_SIZE_COLUMNS = 64;
    private static final int MIN_CONSOLE_SIZE_ROWS = 20;
    private static final long WORKER_PERIODICITY_MSECS = 10000;             //  worker thread does its work every 10 seconds

    private static final String[] _logReportingBlackList = { SystemProcessor.class.getName(),
                                                             RESTSystemConsole.class.getName() };

    private static final Logger LOGGER = LogManager.getLogger(RESTSystemConsole.class);

    private final Listener _listener;
    private final String _name;
    private final Map<String, SessionInfo> _sessions = new HashMap<>();
    private final String _webDirectory;
    private WorkerThread _workerThread = null;

    //  Most recent read-only messages and log entries and all read-reply messages,
    //  held here so we can fill out a new session
    private final List<KomodoLoggingAppender.LogEntry> _cachedLogEntries = new LinkedList<>();
    private final Queue<String> _cachedReadOnlyMessages = new LinkedList<>();
    private final List<PendingReadReplyMessage> _cachedReadReplyMessages = new LinkedList<>();

    //  Input messages we've received from the client(s), but which have not yet been delivered to the operating system.
    //  Key is the session identifier of the client which sent it to us; value is the actual message.
    private final Map<String, String> _pendingInputMessages = new LinkedHashMap<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param name node name of the SP
     */
    public RESTSystemConsole(
        final String name
    ) {
        _name = name;
        _listener = new Listener(443);
        _webDirectory = PathNames.RESOURCES_ROOT_DIRECTORY + "web/";
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract classes for the various handlers and handler threads
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * One of these exists per endpoint - the implementing class is endpoint-specific,
     * and knows how to construct an endpoint-specific handler thread to handle each HTTP request for the endpoint.
     */
    private static abstract class DelegatingHttpHandler implements HttpHandler {

        public void handle(
            final HttpExchange exchange
        ) {
            LOGGER.traceEntry(String.format("<-- %s %s", exchange.getRequestMethod(), exchange.getRequestURI()));
            delegate(exchange);
        }

        public abstract void delegate(
            final HttpExchange exchange
        );
    }

    /**
     * We get a new one of these for every HTTP request that comes in.
     */
    private abstract class HandlerThread extends Thread {

        final HttpExchange _exchange;
        final InputStream _requestBody;
        final Headers _requestHeaders;
        final String _requestMethod;

        public HandlerThread(
            final HttpExchange exchange
        ) {
            _exchange = exchange;
            _requestBody = _exchange.getRequestBody();
            _requestHeaders = _exchange.getRequestHeaders();
            _requestMethod = _exchange.getRequestMethod();
        }

        @Override
        public abstract void run();

        /**
         * Checks the headers for a client id, then locates the corresponding SessionInfo object.
         * Returns null if SessionInfo object is not found or is unspecified.
         * Serves as validation for clients which have presumably previously done a POST to /session
         */
        SessionInfo findClient(
        ) {
            List<String> values = _requestHeaders.get("Client");
            if ((values != null) && (values.size() == 1)) {
                String clientId = values.get(0);
                synchronized (_sessions) {
                    SessionInfo sessionInfo = _sessions.get(clientId);
                    if (sessionInfo != null) {
                        return sessionInfo;
                    }
                }
            }

            return null;
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

        /**
         * Convenient method for handling the situation where a method is requested which is not supported on the endpoint
         */
        void respondBadMethod() {
            String response = String.format("Method %s is not supported for the given endpoint\n", _requestMethod);
            respondWithText(HttpURLConnection.HTTP_BAD_METHOD, response);
        }

        /**
         * Convenient method for handling the situation where a particular request was in error.
         */
        void respondBadRequest(
            final String explanation
        ) {
            respondWithText(HttpURLConnection.HTTP_BAD_REQUEST, explanation + "\n");
        }

        /**
         * Convenient method for handling the situation where no session exists
         */
        void respondNoSession() {
            String response = "Forbidden - session not established\n";
            respondWithText(HttpURLConnection.HTTP_FORBIDDEN, response);
        }

        /**
         * Convenient method for handling an internal server error
         */
        void respondServerError(
            final String message
        ) {
            respondWithText(HttpURLConnection.HTTP_INTERNAL_ERROR, message + "\n");
        }

        /**
         * Convenient method for setting up a 401 response
         */
        void respondUnauthorized() {
            String response = "Unauthorized - credentials not provided or are invalid\nPlease enter credentials\n";
            respondWithText(HttpURLConnection.HTTP_UNAUTHORIZED, response);
        }

        /**
         * For responding to the client with the content of a binary file
         */
        void respondWithBinaryFile(
            final int code,
            final String mimeType,
            final String fileName
        ) {
            LOGGER.traceEntry(String.format("respondWithBinaryFile - code:%d mimeType:%s fileName:%s", code, mimeType, fileName));
            byte[] bytes;
            try {
                bytes = Files.readAllBytes(Paths.get(fileName));
            } catch (IOException ex) {
                LOGGER.catching(ex);
                bytes = String.format("Cannot find requested file '%s'", fileName).getBytes();
                try {
                    _exchange.sendResponseHeaders(code, bytes.length);
                    OutputStream os = _exchange.getResponseBody();
                    os.write(bytes);
                    _exchange.close();
                    return;
                } catch (IOException ex2) {
                    LOGGER.catching(ex2);
                    _exchange.close();
                    return;
                }
            }

            _exchange.getResponseHeaders().add("content-type", mimeType);
            _exchange.getResponseHeaders().add("Cache-Control", "no-store");
            try {
                _exchange.sendResponseHeaders(code, bytes.length);
                OutputStream os = _exchange.getResponseBody();
                os.write(bytes);
            } catch (Exception ex) {
                LOGGER.catching(ex);
            } finally {
                _exchange.close();
            }
        }

        /**
         * Convenient method for sending responses containing JSON
         * @param code The response code - 200, 201, 403, etc - most responses >= 300 won't necessarily have a JSON formatted body
         */
        void respondWithJSON(
            final int code,
            final Object object
        ) {
            LOGGER.traceEntry(String.format("code:%d object:%s", code, object.toString()));
            try {
                ObjectMapper mapper = new ObjectMapper();
                String content = mapper.writeValueAsString(object);
                System.out.println("-->" + content);   //TODO remove
                LOGGER.trace(String.format("  JSON:%s", content));
                _exchange.getResponseHeaders().add("Content-type", "application/json");
                _exchange.getResponseHeaders().add("Cache-Control", "no-store");
                _exchange.sendResponseHeaders(code, content.length());
                OutputStream os = _exchange.getResponseBody();
                os.write(content.getBytes());
                os.close();
            } catch (IOException ex) {
                LOGGER.catching(ex);
                _exchange.close();
            }
        }

        /**
         * Convenient method for sending responses containing straight text
         * @param code The response code - 200, 201, 403, etc
         */
        void respondWithText(
            final int code,
            final String content
        ) {
            LOGGER.traceEntry(String.format("code:%d content:%s", code, content));
            System.out.println("-->" + content);   //TODO remove

            _exchange.getResponseHeaders().add("Content-type", "text/plain");
            byte[] bytes = content.getBytes();

            try {
                _exchange.sendResponseHeaders(code, bytes.length);
                OutputStream os = _exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (IOException ex) {
                LOGGER.catching(ex);
                _exchange.close();
            }
        }

        /**
         * When we need to send back a text file
         */
        void respondWithTextFile(
            final int code,
            final String mimeType,
            final String fileName
        ) {
            LOGGER.traceEntry(String.format("respondWithTextFile - code:%d mimeType:%s fileName:%s", code, mimeType, fileName));
            List<String> textLines;
            try {
                textLines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                LOGGER.catching(ex);
                byte[] bytes = String.format("Cannot find requested file '%s'", fileName).getBytes();
                try {
                    _exchange.sendResponseHeaders(code, bytes.length);
                    OutputStream os = _exchange.getResponseBody();
                    os.write(bytes);
                    return;
                } catch (IOException ex2) {
                    LOGGER.catching(ex2);
                    _exchange.close();
                    return;
                }
            }

            byte[] bytes = String.join("\r\n", textLines).getBytes();
            _exchange.getResponseHeaders().add("content-type", mimeType);
            _exchange.getResponseHeaders().add("Cache-Control", "no-store");
            try {
                _exchange.sendResponseHeaders(code, bytes.length);
                OutputStream os = _exchange.getResponseBody();
                os.write(bytes);
            } catch (Exception ex) {
                LOGGER.catching(ex);
            } finally {
                _exchange.close();
            }
        }

        /**
         * Validate the credentials in the header of the given exchange object.
         * Only for POST to /session.
         * @return true if credentials are valid, else false
         */
        boolean validateCredentials() {
            List<String> values = _requestHeaders.get("Authorization");
            if ((values != null) && (values.size() == 1)) {
                String[] split = values.get(0).split(" ");
                if (split.length == 2) {
                    if (split[0].equalsIgnoreCase("Basic")) {
                        String unBased = new String(Base64.getDecoder().decode(split[1]));
                        String[] unBasedSplit = unBased.split(":");
                        if (unBasedSplit.length == 2) {
                            String givenUserName = unBasedSplit[0];
                            String givenClearTextPassword = unBasedSplit[1];
                            SystemProcessor sp = SystemProcessor.getInstance();
                            SoftwareConfiguration sc = sp.getSoftwareConfiguration();
                            if (givenUserName.equalsIgnoreCase(sc._adminCredentials._userName)) {
                                return sc._adminCredentials.validatePassword(givenClearTextPassword);
                            }
                        }
                    }
                }
            }

            return false;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Endpoint handlers, to be attached to the HTTP listeners
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Handles requests against the /jumpkeys path
     * GET retrieves the current settings as a WORD36 wrapped in a long.
     * PUT accepts the JK settings as a WORD36 wrapped in a long, and persists them to the singular system jump key panel.
     * Either way, JK36 is in the least-significant bit and JKn is 36-n bits to the left of the LSB.
     */
    private class APIJumpKeysHandler extends DelegatingHttpHandler {

        public void delegate(
            final HttpExchange exchange
        ) {
            Thread t = new LocalThread(exchange);
            t.start();
        }

        private class LocalThread extends HandlerThread {

            LocalThread(
                final HttpExchange exchange
            ) {
                super(exchange);
            }

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
            public void run() {
                try {
                    SessionInfo sessionInfo = findClient();
                    if (sessionInfo == null) {
                        respondNoSession();
                        return;
                    }

                    sessionInfo._lastActivity = System.currentTimeMillis();

                    if (_requestMethod.equalsIgnoreCase(HttpMethod.GET._value)) {
                        //  For GET - return the settings as both a composite value and a map of individual jump key settings
                        SystemProcessor sp = SystemProcessor.getInstance();
                        JumpKeys jumpKeysResponse = createJumpKeys(sp.getJumpKeys().getW());
                        respondWithJSON(HttpURLConnection.HTTP_OK, jumpKeysResponse);
                    } else if (_requestMethod.equalsIgnoreCase(HttpMethod.PUT._value)) {
                        //  For PUT - accept the input object - if it has a composite value, use that to set the entire jump key panel.
                        //  If it has no composite value, but it has component values, use them to individually set the jump keys.
                        //  If it has neither, reject the PUT.
                        SystemProcessor sp = SystemProcessor.getInstance();
                        JumpKeys content;
                        try {
                            content = new ObjectMapper().readValue(_requestBody, JumpKeys.class);
                        } catch (IOException ex) {
                            respondBadRequest(ex.getMessage());
                            return;
                        }

                        JumpKeys jumpKeysResponse = null;
                        if (content._compositeValue != null) {
                            if ((content._compositeValue < 0) || (content._compositeValue > 0_777777_777777L)) {
                                respondBadRequest("Invalid composite value");
                                return;
                            }

                            sp.setJumpKeys(new Word36(content._compositeValue));
                            jumpKeysResponse = createJumpKeys(content._compositeValue);
                        } else if (content._componentValues != null) {
                            for (Map.Entry<Integer, Boolean> entry : content._componentValues.entrySet()) {
                                int jumpKeyId = entry.getKey();
                                if ((jumpKeyId < 1) || (jumpKeyId > 36)) {
                                    respondBadRequest(String.format("Invalid component value jump key id: %d", jumpKeyId));
                                    return;
                                }

                                boolean setting = entry.getValue();
                                sp.setJumpKey(jumpKeyId, setting);

                                jumpKeysResponse = createJumpKeys(sp.getJumpKeys().getW());
                                respondWithJSON(HttpURLConnection.HTTP_OK, jumpKeysResponse);
                                return;
                            }
                        } else {
                            respondBadRequest("Requires either composite or component values");
                        }

                        if (jumpKeysResponse != null) {
                            respondWithJSON(HttpURLConnection.HTTP_OK, jumpKeysResponse);
                            long jkValue = jumpKeysResponse._compositeValue;
                            pokeClients(sInfo -> {if (sInfo != sessionInfo) sInfo._pollResult._jumpKeySettings = jkValue;});
                        }
                    } else {
                        //  Neither a GET or a PUT - this is not allowed.
                        respondBadMethod();
                    }
                } catch (Throwable t) {
                    LOGGER.catching(t);
                    respondServerError(getStackTrace(t));
                }
            }
        }
    }

    /**
     * Provides a method for injecting input to the system via POST to /message
     */
    private class APIMessageHandler extends DelegatingHttpHandler {

        public void delegate(
            final HttpExchange exchange
        ) {
            Thread t = new LocalThread(exchange);
            t.start();
        }

        private class LocalThread extends HandlerThread {

            LocalThread(
                final HttpExchange exchange
            ) {
                super(exchange);
            }

            @Override
            public void run() {
                try {
                    SessionInfo sessionInfo = findClient();
                    if (sessionInfo == null) {
                        respondNoSession();
                        return;
                    }

                    sessionInfo._lastActivity = System.currentTimeMillis();
                    if (!_requestMethod.equals(HttpMethod.POST._value)) {
                        respondBadMethod();
                        return;
                    }

                    boolean collision = false;
                    synchronized (_pendingInputMessages) {
                        if (_pendingInputMessages.containsKey(sessionInfo._clientId)) {
                            collision = true;
                        } else {
                            ObjectMapper mapper = new ObjectMapper();
                            InputMessage msg = mapper.readValue(_requestBody, InputMessage.class);

                            //  Ignore empty input, and trim the front and back of non-empty input.
                            String inputText = msg._text.trim();
                            if (!inputText.isEmpty()) {
                                _pendingInputMessages.put(sessionInfo._clientId, inputText);
                            }
                        }
                    }

                    if (collision) {
                        respondWithText(HttpURLConnection.HTTP_CONFLICT, "Previous input not yet acknowledged");
                    } else {
                        respondWithText(HttpURLConnection.HTTP_CREATED, "");
                    }
                } catch (IOException ex) {
                    respondBadRequest("Badly-formatted body");
                } catch (Throwable t) {
                    LOGGER.catching(t);
                    respondServerError(getStackTrace(t));
                }
            }
        }
    }

    /**
     * Handle a poll request (a GET to /poll).
     * Check to see if there is anything new.  If so, send it.
     * Otherwise, wait for some period of time to see whether anything new pops up.
     */
    private class APIPollRequestHandler extends DelegatingHttpHandler {

        public void delegate(
            final HttpExchange exchange
        ) {
            Thread t = new LocalThread(exchange);
            t.start();
        }

        private class LocalThread extends HandlerThread {

            LocalThread(
                final HttpExchange exchange
            ) {
                super(exchange);
            }

            @Override
            public void run() {
                try {
                    SessionInfo sessionInfo = findClient();
                    if (sessionInfo == null) {
                        respondNoSession();
                        return;
                    }

                    sessionInfo._lastActivity = System.currentTimeMillis();
                    if (!_requestMethod.equals(HttpMethod.GET._value)) {
                        respondBadMethod();
                        return;
                    }

                    //  Check if there are any updates already waiting for the client to pick up.
                    //  If not, go into a wait loop which will be interrupted if any updates eventuate during the wait.
                    //  At the end of the wait construct and return a SystemProcessorPollResult object
                    synchronized (sessionInfo) {
                        if (!sessionInfo.hasUpdates()) {
                            try {
                                sessionInfo.wait(POLL_WAIT_MSECS);
                            } catch (InterruptedException ex) {
                                LOGGER.catching(ex);
                            }
                        }

                        respondWithJSON(HttpURLConnection.HTTP_OK, sessionInfo._pollResult);
                        sessionInfo._pollResult = new PollResult();
                    }
                } catch (Throwable t) {
                    LOGGER.catching(t);
                    respondServerError(getStackTrace(t));
                }
            }
        }
    }

    /**
     * Handle posts to /session
     * Validates credentials and method
     * Creates and stashes a SessionInfo record for future method calls
     */
    private class APISessionRequestHandler extends DelegatingHttpHandler {

        public void delegate(
            final HttpExchange exchange
        ) {
            Thread t = new LocalThread(exchange);
            t.start();
        }

        private class LocalThread extends HandlerThread {

            LocalThread(
                final HttpExchange exchange
            ) {
                super(exchange);
            }

            @Override
            public void run() {
                try {
                    if (!validateCredentials()) {
                        respondUnauthorized();
                        return;
                    }

                    if (!_requestMethod.equalsIgnoreCase(HttpMethod.POST._value)) {
                        respondBadMethod();
                        return;
                    }

                    ObjectMapper mapper = new ObjectMapper();
                    ClientAttributes attr = mapper.readValue(_requestBody, ClientAttributes.class);
                    if ((attr._screenSizeColumns < MIN_CONSOLE_SIZE_COLUMNS)
                        || (attr._screenSizeColumns > MAX_CONSOLE_SIZE_COLUMNS)
                        || (attr._screenSizeRows < MIN_CONSOLE_SIZE_ROWS)
                        || (attr._screenSizeRows > MAX_CONSOLE_SIZE_ROWS)) {
                        respondBadRequest("Invalid screen size specified");
                        return;
                    }

                    String clientId = UUID.randomUUID().toString();
                    SessionInfo sessionInfo = new SessionInfo(clientId, attr);

                    synchronized (_cachedLogEntries) {
                        List<SystemLogEntry> sles = new LinkedList<>();
                        for (KomodoLoggingAppender.LogEntry le : _cachedLogEntries) {
                            sles.add(new SystemLogEntry(le));
                        }
                        sessionInfo.appendLogEntries(sles);
                    }

                    sessionInfo._remoteAddress = _exchange.getRemoteAddress();
                    List<OutputMessage> omList = new LinkedList<>();
                    omList.add(OutputMessage.createClearScreenMessage());

                    synchronized (_cachedReadOnlyMessages) {
                        for (String message : _cachedReadOnlyMessages) {
                            omList.add(OutputMessage.createDeleteRowMessage(0));
                            omList.add(OutputMessage.createWriteRowMessage(attr._screenSizeRows - 1,
                                                                           READ_ONLY_FG_COLOR,
                                                                           READ_ONLY_BG_COLOR,
                                                                           message,
                                                                           false));
                        }
                    }

                    synchronized (_cachedReadReplyMessages) {
                        for (PendingReadReplyMessage prrm : _cachedReadReplyMessages) {
                            String text = String.format("%d %s", prrm._messageId, prrm._text);
                            omList.add(OutputMessage.createDeleteRowMessage(0));
                            omList.add(OutputMessage.createWriteRowMessage(attr._screenSizeRows - 1,
                                                                           READ_REPLY_FG_COLOR,
                                                                           READ_REPLY_BG_COLOR,
                                                                           text,
                                                                           false));
                        }
                    }

                    sessionInfo._pollResult._outputMessages = omList.toArray(new OutputMessage[0]);
                    sessionInfo._pollResult._jumpKeySettings = SystemProcessor.getInstance().getJumpKeys().getW();
                    synchronized (_sessions) {
                        _sessions.put(clientId, sessionInfo);
                    }
                    respondWithJSON(HttpURLConnection.HTTP_CREATED, clientId);
                } catch (Throwable t) {
                    LOGGER.catching(t);
                    respondServerError(getStackTrace(t));
                }
            }
        }
    }

    /**
     * Handle all the web endpoint requests
     */
    private class WebHandler extends DelegatingHttpHandler {

        public void delegate(
            final HttpExchange exchange
        ) {
            Thread t = new LocalThread(exchange);
            t.start();
        }

        private class LocalThread extends HandlerThread {

            LocalThread(
                final HttpExchange exchange
            ) {
                super(exchange);
            }

            @Override
            public void run() {
                try {
                    String fileName = _exchange.getRequestURI().getPath();
                    if (fileName.startsWith("/")) {
                        fileName = fileName.substring(1);
                    }
                    if (fileName.isEmpty() || fileName.equalsIgnoreCase("index.html")) {
                        fileName = HTML_FILE_NAME;
                    }

                    String mimeType;
                    boolean textFile = false;
                    if (fileName.contains("favicon.ico")) {
                        fileName = FAVICON_FILE_NAME;
                        mimeType = "image/x-icon";
                    } else {
                        if (fileName.endsWith(".html")) {
                            mimeType = "text/html";
                            textFile = true;
                        } else if (fileName.endsWith(".css")) {
                            mimeType = "text/css";
                            textFile = true;
                        } else if (fileName.endsWith(".bmp")) {
                            mimeType = "image/bmp";
                            textFile = false;
                        } else if (fileName.endsWith(".png")) {
                            mimeType = "image/png";
                            textFile = false;
                        } else if (fileName.endsWith(".js")) {
                            mimeType = "application/javascript";
                            textFile = true;
                        } else if (fileName.endsWith(".json")) {
                            mimeType = "text/json";
                            textFile = true;
                        } else {
                            mimeType = "application/octet-stream";
                        }
                    }

                    String fullName = String.format("%s%s", _webDirectory, fileName);
                    LOGGER.trace("fullName:" + fullName);//TODO remove
                    if (textFile) {
                        respondWithTextFile(HttpURLConnection.HTTP_OK, mimeType, fullName);
                    } else {
                        respondWithBinaryFile(HttpURLConnection.HTTP_OK, mimeType, fullName);
                    }
                } catch (Throwable t) {
                    LOGGER.catching(t);
                }
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  HTTP listener for the API
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
            appendHandler("/", new WebHandler());
            appendHandler("/jumpkeys", new APIJumpKeysHandler());
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
            pokeClients(sessionInfo -> sessionInfo._pollResult = new PollResult());
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Thread-safe method for invoking a particular function on all established clients, then waking them up
     * @param pokeFunction A (potentially anonymous) class containing a function to be executed for eacah entity - if null, it is ignored
     */
    private void pokeClients(
        final PokeClientFunction pokeFunction
    ) {
        Set<SessionInfo> sinfos;
        synchronized (_sessions) {
            sinfos = new HashSet<>(_sessions.values());
        }

        for (SessionInfo sinfo : sinfos) {
            synchronized (sinfo) {
                if (pokeFunction != null) {
                    pokeFunction.function(sinfo);
                }
                sinfo.notify();
            }
        }
    }

    /**
     * Client wants us to age-out any old client info objects
     */
    private void pruneClients() {
        synchronized (_sessions) {
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, SessionInfo>> iter = _sessions.entrySet().iterator();
            List<SessionInfo> removedSessionInfos = new LinkedList<>();
            while (iter.hasNext()) {
                Map.Entry<String, SessionInfo> entry = iter.next();
                SessionInfo sinfo = entry.getValue();
                if (now > (sinfo._lastActivity + CLIENT_AGE_OUT_MSECS)) {
                    LOGGER.info(String.format("Removing aged-out client %s", sinfo._clientId));
                    iter.remove();
                    removedSessionInfos.add(sinfo);
                }
            }

            for (SessionInfo sinfo : removedSessionInfos) {
                synchronized (sinfo) {
                    sinfo._pollResult = new PollResult();
                    sinfo.notify();
                }
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Implementations / overrides of abstract base methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For notifying clients that a pending ReadReplyMessage is no longer pending,
     * at least insofar as the operating system is concerned.
     * Invoked by the SystemProcessor.
     */
    public void cancelReadReplyMessage(
        final int messageId
    ) {
        synchronized (_cachedReadReplyMessages) {
            _cachedReadReplyMessages.remove(messageId);
        }
        pokeClients(sessionInfo -> sessionInfo.cancelReadReplyMessage(messageId));
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
            writer.write(String.format("  APIListener commonName=%s portNumber=%d\n",
                                       _listener.getCommonName(),
                                       _listener.getPortNumber()));

            writer.write("  Pending input messages:\n");
            synchronized (_pendingInputMessages) {
                for (Map.Entry<String, String> entry : _pendingInputMessages.entrySet()) {
                    String clientId = entry.getKey();
                    String text = entry.getValue();
                    writer.write(String.format("    clientId=%s:'%s'\n", clientId, text));
                }
            }

            long now = System.currentTimeMillis();
            synchronized (_sessions) {
                for (SessionInfo sinfo : _sessions.values()) {
                    synchronized (sinfo) {
                        writer.write(String.format("  Client   %sRemote Address:%s   Last Activity %d msec ago\n",
                                                   sinfo._isMaster ? "MASTER " : "",
                                                   sinfo._remoteAddress.getAddress().getHostAddress(),
                                                   now - sinfo._lastActivity));
                        //TODO whatever from the pollInfo object
                    }
                }
            }
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * SystemProcessor calls here to see if there is an input message waiting to be passed along.
     * If we find one, construct an output message for the session it came from, to unlock that client's keyboard.
     */
    @Override
    public String pollInputMessage() {
        Map.Entry<String, String> nextEntry = null;
        synchronized (_pendingInputMessages) {
            Iterator<Map.Entry<String, String>> iter = _pendingInputMessages.entrySet().iterator();
            if (iter.hasNext()) {
                nextEntry = iter.next();
                iter.remove();
            }
        }

        if (nextEntry != null) {
            SessionInfo sinfo;
            synchronized (_sessions) {
                sinfo = _sessions.get(nextEntry.getKey());
            }
            if (sinfo != null) {
                synchronized (sinfo) {
                    sinfo.postInputMessage(nextEntry.getValue());
                }
            }

            return nextEntry.getValue();
        } else {
            return null;
        }
    }

    /**
     * SystemProcessor calls here to post a read-only message
     */
    @Override
    public void postReadOnlyMessage(
        final String message,
        final Boolean rightJustified
    ) {
        synchronized (_cachedReadOnlyMessages) {
            _cachedReadOnlyMessages.add(message);
            while (_cachedReadOnlyMessages.size() > MAX_CACHED_READ_ONLY_MESSAGES) {
                _cachedReadOnlyMessages.poll();
            }
        }
        pokeClients(sessionInfo -> sessionInfo.postReadOnlyMessage(message, rightJustified));
    }

    /**
     * SystemProcessor calls here to post a read-reply message
     */
    @Override
    public void postReadReplyMessage(
        final int messageId,
        final String message,
        final int maxReplyLength
    ) {
        synchronized (_cachedReadReplyMessages) {
            _cachedReadReplyMessages.add(new PendingReadReplyMessage(-1, maxReplyLength, messageId, message));
        }
        pokeClients(sessionInfo -> sessionInfo.postReadReplyMessage(messageId, message, maxReplyLength));
    }

    /**
     * Cache the given status message and notify the pending clients that an updated message is available
     */
    @Override
    public void postStatusMessages(
        final String[] messages
    ) {
        pokeClients(sessionInfo -> sessionInfo.postStatusMessages(messages));
    }

    /**
     * Given a set of log entries, propagate all of the ones which do not come from black-listed sources, to any pending clients.
     * If there are none after filtering, don't annoy the clients.
     */
    @Override
    public void postSystemLogEntries(
        final KomodoLoggingAppender.LogEntry[] logEntries
    ) {
        List<KomodoLoggingAppender.LogEntry> logList = new LinkedList<>();
        for (KomodoLoggingAppender.LogEntry logEntry : logEntries) {
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
            //  Update cache of recent log entries first
            synchronized (_cachedLogEntries) {
                _cachedLogEntries.addAll(logList);
                while (_cachedLogEntries.size() > MAX_CACHED_LOG_ENTRIES) {
                    _cachedLogEntries.remove(0);
                }
            }

            //  Now notify any sessions of the new entries
            List<SystemLogEntry> sles = new LinkedList<>();
            for (KomodoLoggingAppender.LogEntry kle : logList) {
                sles.add(new SystemLogEntry(kle));
            }

            pokeClients(sessionInfo -> sessionInfo.appendLogEntries(sles));
        }
    }

    /**
     * Reset all of the connected console sessions
     */
    @Override
    public void reset() {
        pokeClients(SessionInfo::resetClient);
    }

    /**
     * Starts this entity
     */
    @Override
    public boolean start(
    ) {
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

    /**
     * Stops this entity
     */
    @Override
    public void stop() {
        _listener.stop();
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
