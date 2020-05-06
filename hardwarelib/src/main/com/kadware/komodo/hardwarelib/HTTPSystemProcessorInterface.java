/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.HttpMethod;
import com.kadware.komodo.baselib.KomodoLoggingAppender;
import com.kadware.komodo.baselib.PathNames;
import com.kadware.komodo.baselib.SecureWebServer;
import com.kadware.komodo.baselib.WebServer;
import com.kadware.komodo.baselib.Word36;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.Queue;
import java.util.concurrent.ScheduledFuture;
import static java.util.concurrent.TimeUnit.SECONDS;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.EntryMessage;


/**
 * Class which implements the functionality necessary for a system console.
 * This variation implements an HTTP server interface, providing all the functionality required of a system console
 * via methods (i.e., DELETE, GET, POST, PUT).
 *
 * This implementation uses long polling.
 * Keeping it simple - we require clients to implement a 24x80 output screen, with a separate input text facility.
 * This can change by altering the screen size constants here; the client should be implemented such that it can
 * easily be changed as well.
 */
@SuppressWarnings("Duplicates")
public class HTTPSystemProcessorInterface implements SystemProcessorInterface {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  24-bit RGB color designations for OS console messages
    private static final int INPUT_FG_COLOR = 0xffffff;
    private static final int INPUT_BG_COLOR = 0x000000;
    private static final int READ_ONLY_FG_COLOR = 0x00ff00;
    private static final int READ_ONLY_BG_COLOR = 0x000000;
    private static final int READ_REPLY_FG_COLOR = 0xffff00;
    private static final int READ_REPLY_BG_COLOR = 0x000000;
    private static final int STATUS_FG_COLOR = 0x00ffff;
    private static final int STATUS_BG_COLOR = 0x000000;

    private static final long CLIENT_AGE_OUT_MSECS = 10 * 60 * 1000;        //  10 minutes of no polling ages out a client
    private static final long DEFAULT_POLL_WAIT_MSECS = 10000;              //  10 second (maximum) poll delay as a default
    private static final String FAVICON_FILE_NAME = "favicon.png";          //  relative to web directory
    private static final int MAX_CACHED_LOG_ENTRIES = 200;                  //  max size of most-recent log entries
    private static final int MAX_CACHED_READ_ONLY_MESSAGES = 30;            //  max size of container of most-recent RO messages
    private static final int MAX_CONSOLE_SIZE_COLUMNS = 132;
    private static final int MAX_CONSOLE_SIZE_ROWS = 50;
    private static final int MIN_CONSOLE_SIZE_COLUMNS = 64;
    private static final int MIN_CONSOLE_SIZE_ROWS = 20;
    private static final long PRUNER_PERIODICITY_SECONDS = 60;

    private static final String KEYSTORE_FILENAME = "keystore.jks";         //  relative to web directory
    private static final String KEYSTORE_PASSWORD = "komodo";
    private static final String KEYENTRY_ALIAS = "default";
    private static final String KEYENTRY_PASSWORD = "komodo";

    private static final Pattern INCLUDE_REXEC_PATTERN =
        Pattern.compile("<!--\\s*#include\\s+file\\s*=\\s*\"[^\"]*\"\\s*-->");

    private static final String[] _logReportingBlackList = { SystemProcessor.class.getSimpleName(),
                                                             HTTPSystemProcessorInterface.class.getSimpleName(),
                                                             SCIHttpHandler.class.getSimpleName(),
                                                             SessionInfo.class.getSimpleName(),
                                                             Pruner.class.getSimpleName(),
                                                             APIJumpKeysHandler.class.getSimpleName(),
                                                             APIMessageHandler.class.getSimpleName(),
                                                             APIPollRequestHandler.class.getSimpleName(),
                                                             APISessionRequestHandler.class.getSimpleName(),
                                                             WebHandler.class.getSimpleName(),
                                                             HttpListener.class.getSimpleName(),
                                                             HttpsListener.class.getSimpleName() };

    private static final Logger LOGGER = LogManager.getLogger(HTTPSystemProcessorInterface.class.getSimpleName());

    private final String _keystoreDirectory;
    private HttpListener _httpListener;
    private HttpsListener _httpsListener;
    private final String _name;
    private final Pruner _pruner = new Pruner();
    private final Map<String, SessionInfo> _sessions = new HashMap<>();
    private final SystemProcessor _parentSystemProcessor;
    private final String _webDirectory;

    //  Most recent read-only messages and log entries and all read-reply messages,
    //  held here so we can fill out a new session
    private final List<KomodoLoggingAppender.LogEntry> _cachedLogEntries = new LinkedList<>();
    private final Queue<String> _cachedReadOnlyMessages = new LinkedList<>();
    private final List<PendingReadReplyMessage> _cachedReadReplyMessages = new LinkedList<>();

    //  Input messages we've received from the client(s), but which have not yet been delivered to the operating system.
    //  Key is the session identifier of the client which sent it to us; value is the actual message.
    private final Map<String, SystemProcessorInterface.ConsoleInputMessage> _pendingInputMessages = new LinkedHashMap<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param parentSystemProcessor reference to the SystemProcessor which contains us
     * @param name Name to be used for the console interface
     * @param httpPort port number for HTTP interface - null to disable
     * @param httpsPort port number for HTTPS interface - null to disable
     */
    public HTTPSystemProcessorInterface(
        final SystemProcessor parentSystemProcessor,
        final String name,
        final Integer httpPort,
        final Integer httpsPort
    ) {
        _parentSystemProcessor = parentSystemProcessor;
        _name = name;
        _httpListener = httpPort == null ? null : new HttpListener(httpPort);
        _httpsListener = httpsPort == null ? null : new HttpsListener(httpsPort);
        _keystoreDirectory = PathNames.WEB_ROOT_DIRECTORY;
        _webDirectory = PathNames.WEB_ROOT_DIRECTORY + "systemProcessorInterface/";
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Communications packets sent between us and the REST client
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Information sent to us by the client when it attempts to establish a session
     */
    private static class ClientAttributes {
        @JsonProperty("consoleScreenSizeColumns")   final int _screenSizeColumns;
        @JsonProperty("consoleScreenSizeRows")      final int _screenSizeRows;

        ClientAttributes(
            @JsonProperty("consoleScreenSizeColumns")   final int screenSizeColumns,
            @JsonProperty("consoleScreenSizeRows")      final int screenSizeRows
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
     * Client issues a GET on the /poll subdirectory, and we respond with one of these objects whenever at least
     * one entity in the poll result has been updated.  Upon return, those items which are arrays may either null
     * or empty if they were not updated.
     */
    private static class PollResult {
        @JsonProperty("jumpKeys")                   public JumpKeys _jumpKeys;
        @JsonProperty("newLogEntries")              public SystemLogEntry[] _logEntries;
        @JsonProperty("outputMessages")             public OutputMessage[] _outputMessages;

        PollResult(
            final JumpKeys jumpKeys,
            final List<SystemLogEntry> logEntries,
            final List<OutputMessage> messages
        ) {
            _jumpKeys = jumpKeys;
            _logEntries = logEntries.toArray(new SystemLogEntry[0]);
            _outputMessages = messages.toArray(new OutputMessage[0]);
        }
    }

    /**
     * We send these to the client to affect what is on its display.
     */
    private static class OutputMessage {

        static final int MESSAGE_TYPE_CLEAR_SCREEN = 0;
        static final int MESSAGE_TYPE_UNLOCK_KEYBOARD = 1;
        static final int MESSAGE_TYPE_DELETE_ROW = 2;
        static final int MESSAGE_TYPE_WRITE_ROW = 3;

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

        @Override
        public String toString() {
            return switch (_messageType) {
                case MESSAGE_TYPE_CLEAR_SCREEN -> "CLEAR_SCREEN";
                case MESSAGE_TYPE_DELETE_ROW -> String.format("DELETE_ROW %d", _rowIndex);
                case MESSAGE_TYPE_UNLOCK_KEYBOARD -> "UNLOCK_KEYBOARD";
                case MESSAGE_TYPE_WRITE_ROW -> String.format("WRITE_ROW %d fg=0x%06x bg=0x%06x right=%s, '%s'",
                                                             _rowIndex,
                                                             _textColor,
                                                             _backgroundColor,
                                                             _rightJustified,
                                                             _text);
                default -> (String.format("Unknown message type %s", _messageType));
            };
        }

        static OutputMessage createClearScreenMessage() {
            return new OutputMessage(MESSAGE_TYPE_CLEAR_SCREEN,
                                     null,
                                     null,
                                     null,
                                     null,
                                     null);
        }

        static OutputMessage createUnlockKeyboardMessage() {
            return new OutputMessage(MESSAGE_TYPE_UNLOCK_KEYBOARD,
                                     null,
                                     null,
                                     null,
                                     null,
                                     null);
        }

        static OutputMessage createDeleteRowMessage(
            final int rowIndex
        ) {
            return new OutputMessage(MESSAGE_TYPE_DELETE_ROW,
                                     rowIndex,
                                     null,
                                     null,
                                     null,
                                     null);
        }

        static OutputMessage createWriteRowMessage(
            final int rowIndex,
            final int textColor,
            final int backgroundColor,
            final String text,
            final Boolean rightJustified
        ) {
            return new OutputMessage(MESSAGE_TYPE_WRITE_ROW,
                                     rowIndex,
                                     textColor,
                                     backgroundColor,
                                     text,
                                     rightJustified);
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
    //  Miscellaneous internal classes
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
     * SessionInfo - information regarding a particular client
     */
    private static class SessionInfo {

        private static int _nextConsoleId = 1;
        private final ClientAttributes _clientAttributes;
        private final String _clientId;         //  Identifier used in cookies to identify this session
        private final int _consoleId;           //  Identifier passed to the OS so it can target responses just to one session
        private boolean _jumpKeysUpdated = false;
        private long _lastActivity = System.currentTimeMillis();
        private final List<SystemLogEntry> _newLogEntries = new LinkedList<>();     //  log entries not yet delivered to client
        private final List<OutputMessage> _newOutputMessages = new LinkedList<>();  //  console messages not yet delivered to client
        private InetSocketAddress _remoteAddress = null;
        private int _statusMessageCount = 0;

        //  Read Reply messages which have not yet been responded to
        //  Key is the messageId, value is a PendingReadReplyMessage object
        private final Map<Integer, PendingReadReplyMessage> _pendingReadReplyMessages = new HashMap<>();

        //  A row is 'pinned' if it contains content which cannot be allowed to scroll off the display.
        //  Examples are status messages and pending read reply messages.
        //  This table is indexed by the corresponding row index (which is the row number minus 1, if anyone cares).
        private final boolean[] _pinnedState;

        private static final Logger LOGGER = LogManager.getLogger(SessionInfo.class.getSimpleName());

        SessionInfo(
            final String clientId,
            final ClientAttributes clientAttributes
        ) {
            _clientAttributes = clientAttributes;
            _clientId = clientId;
            _pinnedState = new boolean[clientAttributes._screenSizeRows];
            synchronized (SessionInfo.class) {
                _consoleId = _nextConsoleId++;
            }
        }

        /**
         * Cancels a read-reply message - generally means that the message has been responded to
         */
        void cancelReadReplyMessage(
            final int messageId,
            final String replacementText
        ) {
            EntryMessage em = LOGGER.traceEntry("cancelReadReplyMessage(messageId={})", messageId);

            PendingReadReplyMessage prrm = _pendingReadReplyMessages.get(messageId);
            if (prrm != null) {
                _newOutputMessages.add(OutputMessage.createWriteRowMessage(prrm._currentRowIndex,
                                                                           READ_ONLY_FG_COLOR,
                                                                           READ_ONLY_BG_COLOR,
                                                                           replacementText,
                                                                           false));
                _pendingReadReplyMessages.remove(messageId);
                _pinnedState[prrm._currentRowIndex] = false;
                LOGGER.traceExit(em);
            }
        }

        void clear() {
            _jumpKeysUpdated = false;
            _newOutputMessages.clear();
            _newLogEntries.clear();
            _pendingReadReplyMessages.clear();
        }

        boolean hasUpdates() {
            return !_newOutputMessages.isEmpty() || !_newLogEntries.isEmpty() || _jumpKeysUpdated ;
        }

        /**
         * Resets the client - caller should notify() on _newOutputMessages
         */
        void resetClient() {
            EntryMessage em = LOGGER.traceEntry("resetClient(messageId={})");

            _pendingReadReplyMessages.clear();
            _newOutputMessages.clear();
            _newOutputMessages.add(OutputMessage.createClearScreenMessage());
            _newOutputMessages.add(OutputMessage.createUnlockKeyboardMessage());

            LOGGER.traceExit(em);
        }

        /**
         * Posts an echo of an input message - caller should notify() on _newOutputMessages
         */
        void postInputMessage(
            final String message
        ) {
            EntryMessage em = LOGGER.traceEntry("postInputMessage(message='{}')", message);

            scroll();
            int rx = _clientAttributes._screenSizeRows - 1;
            _newOutputMessages.add(OutputMessage.createWriteRowMessage(rx,
                                                                       INPUT_FG_COLOR,
                                                                       INPUT_BG_COLOR,
                                                                       message,
                                                                       false));

            LOGGER.traceExit(em);
        }

        /**
         * Posts a read-only message to this client - caller should notify() on _newOutputMessages
         */
        void postReadOnlyMessage(
            final String message,
            final Boolean rightJustified
        ) {
            EntryMessage em = LOGGER.traceEntry("postReadOnlyMessage(message='{}', rightJust={})",
                                                message,
                                                rightJustified);

            scroll();
            int rx = _clientAttributes._screenSizeRows - 1;
            _newOutputMessages.add(OutputMessage.createWriteRowMessage(rx,
                                                                       READ_ONLY_FG_COLOR,
                                                                       READ_ONLY_BG_COLOR,
                                                                       message,
                                                                       rightJustified));

            LOGGER.traceExit(em);
        }

        /**
         * Posts a new read-reply message to the client - caller should notify() on _newOutputMessages
         */
        void postReadReplyMessage(
            final int messageId,
            final String message,
            final int maxReplySize
        ) {
            EntryMessage em = LOGGER.traceEntry("postReadReplyMessage(messageId={} message='{}', maxReplySize={})",
                                                messageId,
                                                message,
                                                maxReplySize);

            if ((messageId >= 0) && (messageId <= 9) && !_pendingReadReplyMessages.containsKey(messageId)) {
                scroll();
                int rx = _clientAttributes._screenSizeRows - 1;
                _newOutputMessages.add(OutputMessage.createWriteRowMessage(rx,
                                                                           READ_REPLY_FG_COLOR,
                                                                           READ_REPLY_BG_COLOR,
                                                                           message,
                                                                           false));
                _pinnedState[rx] = true;
                PendingReadReplyMessage prrm = new PendingReadReplyMessage(rx, maxReplySize, messageId, message);
                _pendingReadReplyMessages.put(messageId, prrm);
            }

            LOGGER.traceExit(em);
        }

        /**
         * Posts a new set of status messages to the client - caller should notify() on _newOutputMessages
         * Note that we have to ensure that we do not accidentally overwrite any pending read-reply messages.
         * If the new set of messages has more content than the previous set, and if we would overwrite one or more
         *      pending read-reply messages, then those messages have to be moved to the bottom of the screen - i.e., they
         *      must be effectively resent.
         * If the new set has less content, we need to overwrite the previous excessive status message rows with blank rows.
         */
        void postStatusMessages(
            final String[] messages
        ) {
            EntryMessage em = LOGGER.traceEntry("postStatusMessages(messages={})",
                                                String.join(" ", messages));

            for (int mx = 0; mx < _statusMessageCount; ++mx) {
                if (mx < messages.length) {
                    _newOutputMessages.add(OutputMessage.createWriteRowMessage(mx,
                                                                               STATUS_FG_COLOR,
                                                                               STATUS_BG_COLOR,
                                                                               messages[mx],
                                                                               false));
                    _pinnedState[mx] = true;
                } else {
                    _newOutputMessages.add(OutputMessage.createWriteRowMessage(mx,
                                                                               READ_ONLY_FG_COLOR,
                                                                               READ_ONLY_BG_COLOR,
                                                                               "",
                                                                               false));
                    _pinnedState[mx] = false;
                }
            }

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

            LOGGER.traceExit(em);
        }

        /**
         * Finds the first delete-able row on the client's display, and effectively deletes it
         * scrolling all rows below it up by one row - caller should notify() on _newOutputMessages
         */
        void scroll() {
            EntryMessage em = LOGGER.traceEntry("scroll()");

            //  Find the index of the first non-pinned row
            int rx = 0;
            while (_pinnedState[rx]) {
                rx++;
            }

            //  Scroll - create client output to implement this visually, then scroll all our internal objects as well.
            _newOutputMessages.add(OutputMessage.createDeleteRowMessage(rx));
            _newOutputMessages.add(OutputMessage.createWriteRowMessage(_clientAttributes._screenSizeRows - 1,
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
            _pinnedState[_clientAttributes._screenSizeRows - 1] = false;

            LOGGER.traceExit(em);
        }
    }

    /**
     * Periodically age-out dead sessions
     */
    private class Pruner {

        private final Logger LOGGER = LogManager.getLogger(Pruner.class.getSimpleName());

        private class RunnablePruner implements Runnable {
            public void run() {
                EntryMessage em = LOGGER.traceEntry("run()");

                List<SessionInfo> sessionsToRemove = new LinkedList<>();
                long now = System.currentTimeMillis();
                synchronized (_sessions) {
                    for (SessionInfo sinfo : _sessions.values()) {
                        if (now > (sinfo._lastActivity + CLIENT_AGE_OUT_MSECS)) {
                            sessionsToRemove.add(sinfo);
                        }
                    }

                    for (SessionInfo sinfo : sessionsToRemove) {
                        _sessions.remove(sinfo._clientId);
                    }
                }

                for (SessionInfo sinfo : sessionsToRemove) {
                    LOGGER.info(String.format("Removed aged-out client session id=%s", sinfo._clientId));
                    synchronized (sinfo) {
                        sinfo.clear();
                        sinfo._newLogEntries.notify();
                        sinfo._newOutputMessages.notify();
                    }
                }

                LOGGER.traceExit(em);
            }
        }

        private final Runnable _runnable = new RunnablePruner();
        private final ScheduledExecutorService _scheduler = Executors.newScheduledThreadPool(1);
        private ScheduledFuture<?> _schedule = null;

        private void start() {
            EntryMessage em = LOGGER.traceEntry("start()");
            _schedule = _scheduler.scheduleWithFixedDelay(_runnable,
                                                          PRUNER_PERIODICITY_SECONDS,
                                                          PRUNER_PERIODICITY_SECONDS,
                                                          SECONDS);
            LOGGER.traceExit(em);
        }

        private void stop() {
            EntryMessage em = LOGGER.traceEntry("stop()");
            _schedule.cancel(false);
            _schedule = null;
            LOGGER.traceExit(em);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract classes for the various handlers and handler threads
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * One of these exists (well, a subclass exists) for each configured endpoint
     */
    private abstract class SCIHttpHandler implements HttpHandler {

        private final Logger LOGGER = LogManager.getLogger(SCIHttpHandler.class.getSimpleName());

        /**
         * Checks the headers for a client id, then locates the corresponding SessionInfo object.
         * Returns null if SessionInfo object is not found or is unspecified.
         * Serves as validation for clients which have presumably previously done a POST to /session
         */
        SessionInfo findClient(
            final Headers requestHeaders
        ) {
            EntryMessage em = LOGGER.traceEntry("findClient()");

            SessionInfo result = null;
            List<String> values = requestHeaders.get("Client");
            if ((values != null) && (values.size() == 1)) {
                String clientId = values.get(0);
                synchronized (_sessions) {
                    result = _sessions.get(clientId);
                }
            }

            LOGGER.traceExit(em, result == null ? null : result._clientId);
            return result;
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
         * Implements a sort of readAllLines() which is aware of our lite version of server-side include semantics.
         * Recursive, so that included html files can include other html files.
         */
        private List<String> readAllLinesWithInclude(
            final String fileName
        ) throws IOException {
            EntryMessage em = LOGGER.traceEntry("readAllLinesWithInclude(fileName={})", fileName);

            List<String> fileStrings = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
            List<String> resultStrings = new LinkedList<>();
            for (String fileString : fileStrings) {
                Matcher matcher = INCLUDE_REXEC_PATTERN.matcher(fileString);
                while (matcher.find()) {
                    int s = matcher.start();// start index of the previous match
                    int e = matcher.end();// offset after the last matched character
                    resultStrings.add(fileString.substring(0, e));

                    String includeString = fileString.substring(s, e);
                    String includeFileName = includeString.split("\"")[1];
                    String includeFullName = String.format("%s%s", _webDirectory, includeFileName);
                    resultStrings.addAll(readAllLinesWithInclude(includeFullName));

                    fileString = fileString.substring(e);
                }
                resultStrings.add(fileString);
            }

            LOGGER.traceExit(em);
            return resultStrings;
        }

        /**
         * Convenient method for handling the situation where a method is requested which is not supported on the endpoint
         */
        void respondBadMethod(
            final HttpExchange exchange,
            final String requestMethodString
        ) {
            String response = String.format("Method %s is not supported for the given endpoint\n", requestMethodString);
            respondWithText(exchange, HttpURLConnection.HTTP_BAD_METHOD, response);
        }

        /**
         * Convenient method for handling the situation where a particular request was in error.
         */
        void respondBadRequest(
            final HttpExchange exchange,
            final String explanation
        ) {
            respondWithText(exchange, HttpURLConnection.HTTP_BAD_REQUEST, explanation + "\n");
        }

        /**
         * Convenient method for handling the situation where no session exists
         */
        void respondNoSession(
            final HttpExchange exchange
        ) {
            String response = "Forbidden - session not established\n";
            respondWithText(exchange, HttpURLConnection.HTTP_FORBIDDEN, response);
        }

        /**
         * Convenient method for handling an internal server error
         */
        void respondServerError(
            final HttpExchange exchange,
            final String message
        ) {
            respondWithText(exchange, HttpURLConnection.HTTP_INTERNAL_ERROR, message + "\n");
        }

        /**
         * Convenient method for setting up a 401 response
         */
        void respondUnauthorized(
            final HttpExchange exchange
        ) {
            String response = "Unauthorized - credentials not provided or are invalid\nPlease enter credentials\n";
            respondWithText(exchange, HttpURLConnection.HTTP_UNAUTHORIZED, response);
        }

        /**
         * For responding to the client with the content of a binary file
         * @param exchange The communications exchange
         * @param mimeType mime type of the file to be sent
         * @param fileName file name on this host machine, to be sent
         */
        void respondWithBinaryFile(
            final HttpExchange exchange,
            final String mimeType,
            final String fileName
        ) {
            EntryMessage em = LOGGER.traceEntry("respondWithBinaryFile(mimeType={} fileName={})", mimeType, fileName);

            byte[] bytes;
            try {
                bytes = Files.readAllBytes(Paths.get(fileName));
            } catch (IOException ex) {
                LOGGER.catching(ex);
                bytes = String.format("Cannot find requested file '%s'", fileName).getBytes();
                try {
                    Headers headers = exchange.getResponseHeaders();
                    headers.add("Content-Length", String.valueOf(bytes.length));
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    exchange.close();
                } catch (IOException ex2) {
                    LOGGER.catching(ex2);
                    exchange.close();
                }

                LOGGER.traceExit(em);
                return;
            }

            exchange.getResponseHeaders().add("content-type", mimeType);
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            try {
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception ex) {
                LOGGER.catching(ex);
            }

            exchange.close();
            LOGGER.traceExit(em);
        }

        /**
         * When we need to send back an HTML file (possibly with included other html files)
         * @param exchange The communications exchange
         * @param mimeType mime type of the file to be sent
         * @param fileName file name on this host machine, to be sent
         */
        void respondWithHTMLFile(
            final HttpExchange exchange,
            final String mimeType,
            final String fileName
        ) {
            EntryMessage em = LOGGER.traceEntry("respondWithHTMLFile(mimeType={} fileName={})", mimeType, fileName);

            List<String> textLines;
            try {
                textLines = readAllLinesWithInclude(fileName);
            } catch (IOException ex) {
                LOGGER.catching(ex);
                byte[] bytes = String.format("Cannot find requested file '%s'", fileName).getBytes();
                try {
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                } catch (IOException ex2) {
                    LOGGER.catching(ex2);
                    exchange.close();
                }

                LOGGER.traceExit(em);
                return;
            }

            byte[] bytes = String.join("\r\n", textLines).getBytes();
            exchange.getResponseHeaders().add("content-type", mimeType);
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            try {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Content-Length", String.valueOf(bytes.length));
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception ex) {
                LOGGER.catching(ex);
            }

            exchange.close();
            LOGGER.traceExit(em);
        }

        /**
         * Convenient method for sending responses containing JSON
         * @param exchange The communications exchange
         * @param code The response code - 200, 201, 403, etc - most responses >= 300 won't necessarily have a JSON formatted body
         * @param object the object to be sent
         */
        void respondWithJSON(
            final HttpExchange exchange,
            final int code,
            final Object object
        ) {
            EntryMessage em = LOGGER.traceEntry("respondWithJSON(code={})", code);

            try {
                ObjectMapper mapper = new ObjectMapper();
                String content = mapper.writeValueAsString(object);
                LOGGER.trace("-->" + content);
                exchange.getResponseHeaders().add("Content-type", "application/json");
                exchange.getResponseHeaders().add("Cache-Control", "no-store");
                exchange.sendResponseHeaders(code, content.length());
                OutputStream os = exchange.getResponseBody();
                os.write(content.getBytes());
            } catch (IOException ex) {
                LOGGER.catching(ex);
            }

            exchange.close();
            LOGGER.traceExit(em);
        }

        /**
         * Convenient method for sending responses containing straight text
         * @param exchange The communications exchange
         * @param code The response code - 200, 201, 403, etc
         * @param content The content to be sent
         */
        void respondWithText(
            final HttpExchange exchange,
            final int code,
            final String content
        ) {
            EntryMessage em = LOGGER.traceEntry("respondWithText(code={} content={})", code, content);

            exchange.getResponseHeaders().add("Content-type", "text/plain");
            byte[] bytes = content.getBytes();

            try {
                exchange.sendResponseHeaders(code, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
            } catch (IOException ex) {
                LOGGER.catching(ex);
            }

            exchange.close();
            LOGGER.traceExit(em);
        }

        /**
         * When we need to send back a text file
         * @param exchange The communications exchange
         * @param mimeType mime type of the file to be sent
         * @param fileName file name on this host machine, to be sent
         */
        void respondWithTextFile(
            final HttpExchange exchange,
            final String mimeType,
            final String fileName
        ) {
            EntryMessage em = LOGGER.traceEntry("respondWithTextFile(mimeType={} fileName={})", mimeType, fileName);

            List<String> textLines;
            try {
                textLines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
            } catch (IOException ex) {
                LOGGER.catching(ex);
                byte[] bytes = String.format("Cannot find requested file '%s'", fileName).getBytes();
                try {
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                } catch (IOException ex2) {
                    LOGGER.catching(ex2);
                    exchange.close();
                }

                LOGGER.traceExit(em);
                return;
            }

            byte[] bytes = String.join("\r\n", textLines).getBytes();
            exchange.getResponseHeaders().add("content-type", mimeType);
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            try {
                Headers headers = exchange.getResponseHeaders();
                headers.add("Content-Length", String.valueOf(bytes.length));
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            } catch (Exception ex) {
                LOGGER.catching(ex);
            }

            exchange.close();
            LOGGER.traceExit(em);
        }

        /**
         * Validate the credentials in the header of the given exchange object.
         * Only for POST to /session.
         * @param requestHeaders headers from the Http request
         * @return true if credentials are valid, else false
         */
        boolean validateCredentials(
            final Headers requestHeaders
        ) {
            EntryMessage em = LOGGER.traceEntry("validateCredentials()");

            boolean result = false;
            List<String> values = requestHeaders.get("Authorization");
            if ((values != null) && (values.size() == 1)) {
                String[] split = values.get(0).split(" ");
                if (split.length == 2) {
                    if (split[0].equalsIgnoreCase("Basic")) {
                        String unBased = new String(Base64.getDecoder().decode(split[1]));
                        String[] unBasedSplit = unBased.split(":");
                        if (unBasedSplit.length == 2) {
                            String givenUserName = unBasedSplit[0];
                            String givenClearTextPassword = unBasedSplit[1];
                            if (givenUserName.equalsIgnoreCase(_parentSystemProcessor._credentials._userName)) {
                                result = _parentSystemProcessor._credentials.validatePassword(givenClearTextPassword);
                            }
                        }
                    }
                }
            }

            LOGGER.traceExit(em, result);
            return result;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Endpoint handlers, to be attached to the HTTP listeners
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Handles requests against the /dump path
     */
    private class APIDumpHandler extends SCIHttpHandler {

        private final Logger LOGGER = LogManager.getLogger(APIDumpHandler.class.getSimpleName());

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            EntryMessage em = LOGGER.traceEntry("handle()");

            final Headers requestHeaders = exchange.getRequestHeaders();
            final String requestMethod = exchange.getRequestMethod();
            final String requestURI = exchange.getRequestURI().toString();
            LOGGER.trace("<--" + requestMethod + " " + requestURI);

            BufferedWriter bw = null;
            try {
                SessionInfo sessionInfo = findClient(requestHeaders);
                if (sessionInfo == null) {
                    respondNoSession(exchange);
                    LOGGER.traceExit(em);
                    return;
                }

                sessionInfo._lastActivity = System.currentTimeMillis();

                if (requestMethod.equalsIgnoreCase(HttpMethod.GET._value)) {
                    String fileName = "Komodo-dump-" + Instant.now().toString();
                    String fullName = PathNames.LOGS_ROOT_DIRECTORY + fileName;
                    File f = new File(fullName);
                    //noinspection ResultOfMethodCallIgnored
                    f.createNewFile();
                    FileWriter fw = new FileWriter(f);
                    bw = new BufferedWriter(fw);
                    InventoryManager.getInstance().dump(bw);
                    respondWithText(exchange, HttpURLConnection.HTTP_CREATED, fileName);
                } else {
                    //  Neither a GET or a PUT - this is not allowed.
                    respondBadMethod(exchange, requestMethod);
                }
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            } finally {
                try {
                    if (bw != null) {
                        bw.close();
                    }
                } catch (IOException ex2) {
                    LOGGER.catching(ex2);
                }
            }

            LOGGER.traceExit(em);
        }
    }

    /**
     * Handles requests against the /jumpkeys path
     * GET retrieves the current settings as a WORD36 wrapped in a long.
     * PUT accepts the JK settings as a WORD36 wrapped in a long, and persists them to the singular system jump key panel.
     * Either way, JK36 is in the least-significant bit and JKn is 36-n bits to the left of the LSB.
     */
    private class APIJumpKeysHandler extends SCIHttpHandler {

        private final Logger LOGGER = LogManager.getLogger(APIJumpKeysHandler.class.getSimpleName());

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            EntryMessage em = LOGGER.traceEntry("handle()");

            final InputStream requestBody = exchange.getRequestBody();
            final Headers requestHeaders = exchange.getRequestHeaders();
            final String requestMethod = exchange.getRequestMethod();
            final String requestURI = exchange.getRequestURI().toString();
            LOGGER.trace("<--" + requestMethod + " " + requestURI);

            try {
                SessionInfo sessionInfo = findClient(requestHeaders);
                if (sessionInfo == null) {
                    respondNoSession(exchange);
                    LOGGER.traceExit(em);
                    return;
                }

                sessionInfo._lastActivity = System.currentTimeMillis();

                if (requestMethod.equalsIgnoreCase(HttpMethod.GET._value)) {
                    //  For GET - return the settings as both a composite value and a map of individual jump key settings
                    JumpKeys jumpKeysResponse = createJumpKeys(_parentSystemProcessor.getJumpKeys().getW());
                    respondWithJSON(exchange, HttpURLConnection.HTTP_OK, jumpKeysResponse);
                } else if (requestMethod.equalsIgnoreCase(HttpMethod.PUT._value)) {
                    //  For PUT - accept the input object - if it has a composite value, use that to set the entire jump key panel.
                    //  If it has no composite value, but it has component values, use them to individually set the jump keys.
                    //  If it has neither, reject the PUT.
                    JumpKeys content;
                    try {
                        content = new ObjectMapper().readValue(requestBody, JumpKeys.class);
                    } catch (IOException ex) {
                        respondBadRequest(exchange, ex.getMessage());
                        LOGGER.traceExit(em);
                        return;
                    }

                    JumpKeys jumpKeysResponse = null;
                    if (content._compositeValue != null) {
                        if ((content._compositeValue < 0) || (content._compositeValue > 0_777777_777777L)) {
                            respondBadRequest(exchange, "Invalid composite value");
                            LOGGER.traceExit(em);
                            return;
                        }

                        _parentSystemProcessor.setJumpKeys(new Word36(content._compositeValue));
                        jumpKeysResponse = createJumpKeys(content._compositeValue);
                    } else if (content._componentValues != null) {
                        for (Map.Entry<Integer, Boolean> entry : content._componentValues.entrySet()) {
                            int jumpKeyId = entry.getKey();
                            if ((jumpKeyId < 1) || (jumpKeyId > 36)) {
                                respondBadRequest(exchange, String.format("Invalid component value jump key id: %d", jumpKeyId));
                                LOGGER.traceExit(em);
                                return;
                            }

                            boolean setting = entry.getValue();
                            _parentSystemProcessor.setJumpKey(jumpKeyId, setting);

                            jumpKeysResponse = createJumpKeys(_parentSystemProcessor.getJumpKeys().getW());
                        }
                    } else {
                        respondBadRequest(exchange, "Requires either composite or component values");
                        LOGGER.traceExit(em);
                        return;
                    }

                    //  No need to notify the clients - the SP will call us back and it happens automagically

                    respondWithJSON(exchange, HttpURLConnection.HTTP_OK, jumpKeysResponse);
                    LOGGER.traceExit(em);
                    return;
                } else {
                    //  Neither a GET or a PUT - this is not allowed.
                    respondBadMethod(exchange, requestMethod);
                }
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }

            LOGGER.traceExit(em);
        }
    }

    /**
     * Provides a method for injecting input to the system via POST to /message
     */
    private class APIMessageHandler extends SCIHttpHandler {

        private final Logger LOGGER = LogManager.getLogger(APIMessageHandler.class.getSimpleName());

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            EntryMessage em = LOGGER.traceEntry("handle()");

            final InputStream requestBody = exchange.getRequestBody();
            final Headers requestHeaders = exchange.getRequestHeaders();
            final String requestMethod = exchange.getRequestMethod();
            final String requestURI = exchange.getRequestURI().toString();
            LOGGER.trace("<--" + requestMethod + " " + requestURI);

            try {
                SessionInfo sessionInfo = findClient(requestHeaders);
                if (sessionInfo == null) {
                    respondNoSession(exchange);
                    LOGGER.traceExit(em);
                    return;
                }

                sessionInfo._lastActivity = System.currentTimeMillis();
                if (!requestMethod.equals(HttpMethod.POST._value)) {
                    respondBadMethod(exchange, requestMethod);
                    LOGGER.traceExit(em);
                    return;
                }

                boolean collision = false;
                synchronized (_pendingInputMessages) {
                    if (_pendingInputMessages.containsKey(sessionInfo._clientId)) {
                        collision = true;
                    } else {
                        ObjectMapper mapper = new ObjectMapper();
                        InputMessage msg = mapper.readValue(requestBody, InputMessage.class);

                        //  Ignore empty input, and trim the front and back of non-empty input.
                        String inputText = msg._text.trim();
                        if (!inputText.isEmpty()) {
                            _pendingInputMessages.put(sessionInfo._clientId,
                                                      new SystemProcessorInterface.ConsoleInputMessage(sessionInfo._consoleId, inputText));
                        }

                        _pendingInputMessages.notify();
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
                respondServerError(exchange, getStackTrace(t));
            }

            LOGGER.traceExit(em);
        }
    }

    /**
     * Handle a poll request. Check to see if there is anything new. If so, send it.
     * Otherwise, wait for some period of time to see whether anything new pops up.
     */
    private class APIPollRequestHandler extends SCIHttpHandler {

        private final Logger LOGGER = LogManager.getLogger(APIPollRequestHandler.class.getSimpleName());

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            EntryMessage em = LOGGER.traceEntry("handle()");

            final Headers requestHeaders = exchange.getRequestHeaders();
            final String requestMethod = exchange.getRequestMethod();
            final String requestURI = exchange.getRequestURI().toString();
            LOGGER.trace("<--" + requestMethod + " " + requestURI);

            try {
                SessionInfo sessionInfo = findClient(requestHeaders);
                if (sessionInfo == null) {
                    respondNoSession(exchange);
                    LOGGER.traceExit(em);
                    return;
                }

                sessionInfo._lastActivity = System.currentTimeMillis();
                if (!requestMethod.equals(HttpMethod.GET._value)) {
                    respondBadMethod(exchange, requestMethod);
                    LOGGER.traceExit(em);
                    return;
                }

                //  Check if there are any updates already waiting for the client to pick up.
                //  If not, go into a wait loop which will be interrupted if any updates eventuate during the wait.
                //  At the end of the wait construct and return a SystemProcessorPollResult object
                JumpKeys jks = null;
                List<SystemLogEntry> sles = new LinkedList<>();
                List<OutputMessage> oms = new LinkedList<>();

                synchronized (sessionInfo) {
                    if (!sessionInfo.hasUpdates()) {
                        try {
                            sessionInfo.wait(DEFAULT_POLL_WAIT_MSECS);
                        } catch (InterruptedException ex) {
                            LOGGER.catching(ex);
                        }
                    }

                    if (sessionInfo._jumpKeysUpdated) {
                        jks = createJumpKeys(_parentSystemProcessor.getJumpKeys().getW());
                        sessionInfo._jumpKeysUpdated = false;
                    }

                    if (!sessionInfo._newOutputMessages.isEmpty()) {
                        oms.addAll(sessionInfo._newOutputMessages);
                        sessionInfo._newOutputMessages.clear();
                    }

                    if (!sessionInfo._newLogEntries.isEmpty()) {
                        sles.addAll(sessionInfo._newLogEntries);
                        sessionInfo._newLogEntries.clear();
                    }
                }

                respondWithJSON(exchange, HttpURLConnection.HTTP_OK, new PollResult(jks, sles, oms));
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }

            LOGGER.traceExit(em);
        }
    }

    /**
     * Handle posts to /session
     * Validates credentials and method
     * Creates and stashes a SessionInfo record for future method calls
     */
    private class APISessionRequestHandler extends SCIHttpHandler {

        private final Logger LOGGER = LogManager.getLogger(APISessionRequestHandler.class.getSimpleName());

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            EntryMessage em = LOGGER.traceEntry("handle()");

            final InputStream requestBody = exchange.getRequestBody();
            final Headers requestHeaders = exchange.getRequestHeaders();
            final String requestMethod = exchange.getRequestMethod();
            final String requestURI = exchange.getRequestURI().toString();
            LOGGER.trace("<--" + requestMethod + " " + requestURI);

            try {
                if (!validateCredentials(requestHeaders)) {
                    respondUnauthorized(exchange);
                    LOGGER.traceExit(em);
                    return;
                }

                if (!requestMethod.equalsIgnoreCase(HttpMethod.POST._value)) {
                    respondBadMethod(exchange, requestMethod);
                    LOGGER.traceExit(this.getClass().getName() + "run()");
                    return;
                }

                ObjectMapper mapper = new ObjectMapper();
                ClientAttributes attr = mapper.readValue(requestBody, ClientAttributes.class);
                if ((attr._screenSizeColumns < MIN_CONSOLE_SIZE_COLUMNS)
                    || (attr._screenSizeColumns > MAX_CONSOLE_SIZE_COLUMNS)
                    || (attr._screenSizeRows < MIN_CONSOLE_SIZE_ROWS)
                    || (attr._screenSizeRows > MAX_CONSOLE_SIZE_ROWS)) {
                    respondBadRequest(exchange, "Invalid screen size specified");
                    LOGGER.traceExit(em);
                    return;
                }

                String clientId = UUID.randomUUID().toString();
                SessionInfo sessionInfo = new SessionInfo(clientId, attr);
                sessionInfo._remoteAddress = exchange.getRemoteAddress();
                sessionInfo._jumpKeysUpdated = true;

                List<SystemLogEntry> sles = new LinkedList<>();
                synchronized (_cachedLogEntries) {
                    for (KomodoLoggingAppender.LogEntry le : _cachedLogEntries) {
                        sles.add(new SystemLogEntry(le));
                    }
                }
                sessionInfo._newLogEntries.addAll(sles);

                sessionInfo._newOutputMessages.add(OutputMessage.createClearScreenMessage());
                synchronized (_cachedReadOnlyMessages) {
                    for (String message : _cachedReadOnlyMessages) {
                        sessionInfo._newOutputMessages.add(OutputMessage.createDeleteRowMessage(0));
                        sessionInfo._newOutputMessages.add(OutputMessage.createWriteRowMessage(attr._screenSizeRows - 1,
                                                                                               READ_ONLY_FG_COLOR,
                                                                                               READ_ONLY_BG_COLOR,
                                                                                               message,
                                                                                               false));
                    }
                }

                synchronized (_cachedReadReplyMessages) {
                    for (PendingReadReplyMessage prrm : _cachedReadReplyMessages) {
                        sessionInfo._newOutputMessages.add(OutputMessage.createDeleteRowMessage(0));
                        sessionInfo._newOutputMessages.add(OutputMessage.createWriteRowMessage(attr._screenSizeRows - 1,
                                                                                               READ_REPLY_FG_COLOR,
                                                                                               READ_REPLY_BG_COLOR,
                                                                                               prrm._text,
                                                                                               false));
                    }
                }

                synchronized (_sessions) {
                    _sessions.put(clientId, sessionInfo);
                }

                respondWithJSON(exchange, HttpURLConnection.HTTP_CREATED, clientId);
            } catch (Throwable t) {
                LOGGER.catching(t);
                respondServerError(exchange, getStackTrace(t));
            }

            LOGGER.traceExit(em);
        }
    }

    /**
     * Handle all the web endpoint requests - mostly (solely?) for serving up files.
     */
    private class WebHandler extends SCIHttpHandler {

        private final Logger LOGGER = LogManager.getLogger(SCIHttpHandler.class.getSimpleName());

        @Override
        public void handle(
            final HttpExchange exchange
        ) {
            EntryMessage em = LOGGER.traceEntry("handle()");

            final String requestMethod = exchange.getRequestMethod();
            final String requestURI = exchange.getRequestURI().toString();
            LOGGER.trace("<--" + requestMethod + " " + requestURI);

            try {
                String fileName = exchange.getRequestURI().getPath();
                if (fileName.startsWith("/")) {
                    fileName = fileName.substring(1);
                }

                String mimeType;
                boolean htmlFile = false;
                boolean textFile = false;
                if (fileName.isEmpty()) {
                    fileName = "index.html";
                    mimeType = "text/html";
                    htmlFile = true;
                } else if (fileName.contains("favicon.ico")) {
                    fileName = FAVICON_FILE_NAME;
                    mimeType = "image/x-icon";
                } else if (fileName.endsWith(".html")) {
                    mimeType = "text/html";
                    htmlFile = true;
                } else if (fileName.endsWith(".css")) {
                    mimeType = "text/css";
                    textFile = true;
                } else if (fileName.endsWith(".bmp")) {
                    mimeType = "image/bmp";
                } else if (fileName.endsWith(".png")) {
                    mimeType = "image/png";
                } else if (fileName.endsWith(".js")) {
                    mimeType = "application/javascript";
                } else if (fileName.endsWith(".json")) {
                    mimeType = "text/json";
                    textFile = true;
                } else {
                    mimeType = "application/octet-stream";
                }

                String fullName = String.format("%s%s", _webDirectory, fileName);
                if (htmlFile) {
                    respondWithHTMLFile(exchange, mimeType, fullName);
                } else if (textFile) {
                    respondWithTextFile(exchange, mimeType, fullName);
                } else {
                    respondWithBinaryFile(exchange, mimeType, fullName);
                }
            } catch (Throwable t) {
                LOGGER.catching(t);
            }

            LOGGER.traceExit(em);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  HTTP(s) listeners for the API
    //  All requests must use basic authentication for every message (in the headers, of course)
    //  All requests must include a (supposedly) unique UUID as a client identifier in the headers "Client={uuid}"
    //  This unique UUID must be used for every message sent by a given instance of a client.
    //  ----------------------------------------------------------------------------------------------------------------------------

    private class HttpListener extends WebServer {

        private final Logger LOGGER = LogManager.getLogger(HttpListener.class.getSimpleName());

        /**
         * constructor
         */
        private HttpListener(
            final int portNumber
        ) {
            super(portNumber);
        }

        /**
         * Client wants us to start accepting requests
         */
        @Override
        public void setup(
        ) throws IOException {
            EntryMessage em = LOGGER.traceEntry("setup()");

            super.setup();
            appendHandler("/", new WebHandler());
            appendHandler("/dump", new APIDumpHandler());
            appendHandler("/jumpkeys", new APIJumpKeysHandler());
            appendHandler("/message", new APIMessageHandler());
            appendHandler("/poll", new APIPollRequestHandler());
            appendHandler("/session", new APISessionRequestHandler());
            start();

            LOGGER.traceExit(em);
        }

        /**
         * Owner wants us to stop accepting requests.
         * Tell our base class to STOP, then go wake up all the pending clients.
         */
        @Override
        public void stop() {
            EntryMessage em = LOGGER.traceEntry("setup()");

            super.stop();
            for (SessionInfo sinfo : getSessions()) {
                synchronized (sinfo) {
                    sinfo.clear();
                }
                synchronized (sinfo._newLogEntries) {
                    sinfo._newLogEntries.notify();
                }
                synchronized (sinfo._newOutputMessages) {
                    sinfo._newOutputMessages.notify();
                }
            }

            LOGGER.traceExit(em);
        }
    }

    private class HttpsListener extends SecureWebServer {

        private final Logger LOGGER = LogManager.getLogger(HttpsListener.class.getSimpleName());

        /**
         * constructor
         */
        private HttpsListener(
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
                 IOException,
                 KeyManagementException,
                 KeyStoreException,
                 NoSuchAlgorithmException {
            EntryMessage em = LOGGER.traceEntry("setup()");

            String keystoreFullFilename = _keystoreDirectory + KEYSTORE_FILENAME;
            super.setup(keystoreFullFilename, KEYENTRY_ALIAS, KEYSTORE_PASSWORD, KEYENTRY_PASSWORD);
            appendHandler("/", new WebHandler());
            appendHandler("/dump", new APIDumpHandler());
            appendHandler("/jumpkeys", new APIJumpKeysHandler());
            appendHandler("/message", new APIMessageHandler());
            appendHandler("/poll", new APIPollRequestHandler());
            appendHandler("/session", new APISessionRequestHandler());
            start();

            LOGGER.traceExit(em);
        }

        /**
         * Owner wants us to stop accepting requests.
         * Tell our base class to STOP, then go wake up all the pending clients.
         */
        @Override
        public void stop() {
            EntryMessage em = LOGGER.traceEntry("stop()");

            super.stop();

            for (SessionInfo sinfo : getSessions()) {
                synchronized (sinfo) {
                    sinfo.clear();
                }
                synchronized (sinfo._newLogEntries) {
                    sinfo._newLogEntries.notify();
                }
                synchronized (sinfo._newOutputMessages) {
                    sinfo._newOutputMessages.notify();
                }
            }

            LOGGER.traceExit(em);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static JumpKeys createJumpKeys(
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

    /**
     * Thread-safe way to get a list of all the SessionInfo objects
     */
    public Collection<SessionInfo> getSessions() {
        synchronized (_sessions) {
            return _sessions.values();
        }
    }

    /**
     * Kills the current http listener (if any).
     * If the given port is non-zero, we then create a new http listener and start it up
     * @param httpPort new port number - 0 to disable
     * @return true if successful, else false
     */
    public boolean setNewHttpPort(
        final int httpPort
    ) {
        EntryMessage em = LOGGER.traceEntry("setNewHttpPort(httpPort=%d)", httpPort);

        boolean result = true;
        if (_httpListener != null) {
            _httpListener.stop();
            _httpListener = null;
        }

        if (httpPort != 0) {
            try {
                _httpListener = new HttpListener(httpPort);
                _httpListener.setup();
            } catch (IOException ex) {
                LOGGER.catching(ex);
                result = false;
            }
        }

        LOGGER.traceExit(em, result);
        return result;
    }

    /**
     * Kills the current https listener (if any).
     * If the given port is non-zero, we then create a new https listener and start it up
     * @param httpsPort new port number - 0 to disable
     * @return true if successful, else false
     */
    public boolean setNewHttpsPort(
        final int httpsPort
    ) {
        EntryMessage em = LOGGER.traceEntry("setNewHttpsPort(httpsPort=%d)", httpsPort);

        boolean result = true;
        if (_httpsListener != null) {
            _httpsListener.stop();
            _httpsListener = null;
        }

        if (httpsPort != 0) {
            try {
                _httpsListener = new HttpsListener(httpsPort);
                _httpsListener.setup();
            } catch (IOException
                     | CertificateException
                     | KeyManagementException
                     | KeyStoreException
                     | NoSuchAlgorithmException ex) {
                LOGGER.catching(ex);
                result = false;
            }
        }

        LOGGER.traceExit(em, result);
        return result;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Implementations / overrides of abstract base methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For notifying clients that a pending ReadReplyMessage is no longer pending,
     * at least insofar as the operating system is concerned.
     * Invoked by the SystemProcessor.
     */
    @Override
    public void cancelReadReplyMessage(
        final int consoleId,
        final int messageId,
        final String replacementText
    ) {
        EntryMessage em = LOGGER.traceEntry("cancelReadReplyMessage(messageId=%d)", messageId);

        synchronized (_cachedReadReplyMessages) {
            Iterator<PendingReadReplyMessage> iter = _cachedReadReplyMessages.iterator();
            while (iter.hasNext()) {
                PendingReadReplyMessage prrm = iter.next();
                if (prrm._messageId == messageId) {
                    iter.remove();
                    break;
                }
            }
        }

        for (SessionInfo sinfo : getSessions()) {
            if ((consoleId == 0) || (consoleId == sinfo._consoleId)) {
                synchronized (sinfo) {
                    sinfo.cancelReadReplyMessage(messageId, replacementText);
                    sinfo.notify();
                }
            }
        }

        LOGGER.traceExit(em);
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
            if (_httpListener != null) {
                writer.write(String.format("  HTTPListener portNumber=%d\n",
                                           _httpListener.getPortNumber()));
            }
            if (_httpsListener != null) {
                writer.write(String.format("  HTTPSListener commonName=%s portNumber=%d\n",
                                           _httpsListener.getCommonName(),
                                           _httpsListener.getPortNumber()));
            }

            writer.write("  Pending input messages:\n");
            synchronized (_pendingInputMessages) {
                for (Map.Entry<String, SystemProcessorInterface.ConsoleInputMessage> entry : _pendingInputMessages.entrySet()) {
                    String clientId = entry.getKey();
                    SystemProcessorInterface.ConsoleInputMessage cim = entry.getValue();
                    writer.write(String.format("    clientId=%s:'%s'\n", clientId, cim.toString()));
                }
            }

            long now = System.currentTimeMillis();
            for (SessionInfo sinfo : getSessions()) {
                writer.write(String.format("  Client   Remote Address:%s   Last Activity %d msec ago\n",
                                           sinfo._remoteAddress.getAddress().getHostAddress(),
                                           now - sinfo._lastActivity));
            }
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    @Override
    public String getName() {
        return _name;
    }

    /**
     * SP is telling us that the jump keys have been updated - let our clients know about it.
     */
    @Override
    public void jumpKeysUpdated() {
        for (SessionInfo sinfo : getSessions()) {
            synchronized (sinfo) {
                sinfo._jumpKeysUpdated = true;
                sinfo.notify();
            }
        }
    }

    /**
     * SystemProcessor calls here to see if there is an input message waiting to be passed along.
     * If we find one, construct an output message for the session it came from, to unlock that client's keyboard.
     */
    @Override
    public ConsoleInputMessage pollInputMessage() {
        EntryMessage em = LOGGER.traceEntry("pollInputMessage()");
        ConsoleInputMessage cim = pollInputMessage(0);
        LOGGER.traceExit(em, cim);
        return cim;
    }

    /**
     * SystemProcessor calls here to see if there is an input message waiting to be passed along.
     * If we find one, construct an output message for the session it came from, to unlock that client's keyboard.
     * If there isn't one available, wait for the specified period before returning, to see if one shows up.
     * If timeoutMillis is zero, do not wait.
     */
    @Override
    public ConsoleInputMessage pollInputMessage(
        long timeoutMillis
    ) {
        EntryMessage em = LOGGER.traceEntry("pollInputMessage(timeout={}ms)", timeoutMillis);

        SessionInfo sourceSessionInfo = null;
        SystemProcessorInterface.ConsoleInputMessage cim = null;
        synchronized (_pendingInputMessages) {
            if (_pendingInputMessages.isEmpty()) {
                if (timeoutMillis > 0) {
                    try {
                        _pendingInputMessages.wait(timeoutMillis);
                    } catch (InterruptedException ex) {
                        LOGGER.catching(ex);
                    }
                }
            }

            if (!_pendingInputMessages.isEmpty()) {
                Iterator<Map.Entry<String, SystemProcessorInterface.ConsoleInputMessage>> iter = _pendingInputMessages.entrySet().iterator();
                Map.Entry<String, SystemProcessorInterface.ConsoleInputMessage> nextEntry = iter.next();
                String sourceSessionId = nextEntry.getKey();
                cim = nextEntry.getValue();
                iter.remove();

                sourceSessionInfo = _sessions.get(sourceSessionId);
            }
        }

        if (sourceSessionInfo != null) {
            synchronized (sourceSessionInfo) {
                sourceSessionInfo.postInputMessage(cim._text);
                sourceSessionInfo.notify();
            }
        }

        LOGGER.traceExit(em, cim);
        return cim;
    }

    /**
     * SystemProcessor calls here to post a read-only message
     */
    @Override
    public void postReadOnlyMessage(
        final int consoleId,
        final String message,
        final Boolean rightJustified,
        final Boolean cached
    ) {
        EntryMessage em = LOGGER.traceEntry("postReadOnlyMessage(consoleId={} message='{}' rightJustified={})",
                                            consoleId,
                                            message,
                                            rightJustified);

        if (cached && (consoleId == 0)) {
            synchronized (_cachedReadOnlyMessages) {
                _cachedReadOnlyMessages.add(message);
                while (_cachedReadOnlyMessages.size() > MAX_CACHED_READ_ONLY_MESSAGES) {
                    _cachedReadOnlyMessages.poll();
                }
            }
        }

        for (SessionInfo sinfo : getSessions()) {
            if ((consoleId == 0) || (consoleId == sinfo._consoleId)) {
                synchronized (sinfo) {
                    sinfo.postReadOnlyMessage(message, rightJustified);
                    sinfo.notify();
                }
            }
        }

        LOGGER.traceExit(em);
    }

    /**
     * SystemProcessor calls here to post a read-reply message
     */
    @Override
    public void postReadReplyMessage(
        final int consoleId,
        final int messageId,
        final String message,
        final int maxReplyLength
    ) {
        EntryMessage em = LOGGER.traceEntry("postReadReplyMessage(messageId={} message='{}' maxReplyLength={})",
                                            messageId,
                                            message,
                                            maxReplyLength);

        synchronized (_cachedReadReplyMessages) {
            _cachedReadReplyMessages.add(new PendingReadReplyMessage(-1, maxReplyLength, messageId, message));
        }

        for (SessionInfo sinfo : getSessions()) {
            if ((consoleId == 0) || (consoleId == sinfo._consoleId)) {
                synchronized (sinfo) {
                    sinfo.postReadReplyMessage(messageId, message, maxReplyLength);
                    sinfo.notify();
                }
            }
        }

        LOGGER.traceExit(em);
    }

    /**
     * Cache the given status message and notify the pending clients that an updated message is available
     */
    @Override
    public void postStatusMessages(
        final String[] messages
    ) {
        EntryMessage em = LOGGER.traceEntry("postStatusMessages({})", String.join(",", messages));

        for (SessionInfo sinfo : getSessions()) {
            synchronized (sinfo) {
                sinfo.postStatusMessages(messages);
                sinfo.notify();
            }
        }

        LOGGER.traceExit(em);
    }

    /**
     * Given a set of log entries, propagate all of the ones which do not come from black-listed sources, to any pending clients.
     * If there are none after filtering, don't annoy the clients.
     */
    @Override
    public void postSystemLogEntries(
        final KomodoLoggingAppender.LogEntry[] logEntries
    ) {
        EntryMessage em = LOGGER.traceEntry("postSystemLogEntries()");

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
            synchronized (_cachedLogEntries) {
                //  Update cache of recent log entries first
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

            for (SessionInfo sinfo : getSessions()) {
                synchronized (sinfo) {
                    sinfo._newLogEntries.addAll(sles);
                    sinfo.notify();
                }
            }
        }

        LOGGER.traceExit(em);
    }

    /**
     * Reset all of the connected console sessions
     */
    @Override
    public void reset() {
        EntryMessage em = LOGGER.traceEntry("reset()");

        synchronized (_pendingInputMessages) {
            _pendingInputMessages.clear();
        }

        synchronized (_cachedReadOnlyMessages) {
            _cachedReadOnlyMessages.clear();
        }

        synchronized (_cachedReadReplyMessages) {
            _cachedReadReplyMessages.clear();
        }

        for (SessionInfo sinfo : getSessions()) {
            synchronized (sinfo) {
                sinfo.resetClient();
            }
            synchronized (sinfo._newLogEntries) {
                sinfo._newLogEntries.notify();
            }
            synchronized (sinfo._newOutputMessages) {
                sinfo._newOutputMessages.notify();
            }
        }

        LOGGER.traceExit(em);
    }

    /**
     * Starts this entity
     */
    @Override
    public boolean start(
    ) {
        EntryMessage em = LOGGER.traceEntry("start()");
        try {
            if (_httpListener != null) {
                _httpListener.setup();
            }
            if (_httpsListener != null) {
                _httpsListener.setup();
            }
            _pruner.start();
            LOGGER.traceExit(em, true);
            return true;
        } catch (Exception ex) {
            LOGGER.catching(ex);
            LOGGER.traceExit(em, false);
            return false;
        }
    }

    /**
     * Stops this entity
     */
    @Override
    public void stop() {
        EntryMessage em = LOGGER.traceEntry("stop()");
        if (_httpListener != null) {
            _httpListener.stop();
        }
        if (_httpsListener != null) {
            _httpsListener.stop();
        }
        _pruner.stop();
        LOGGER.traceExit(em);
    }
}
