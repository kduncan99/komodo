/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.KomodoAppender;
import com.kadware.komodo.commlib.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.security.*;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.util.*;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;

//TODO move this somewhere else, where it makes sense
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
//  Notes:
//      Loader code must know how many OS banks exist, and how big they are
//      The loader is responsible for creating the Level 0 BDT (at a minimum)
//      At some point, the loader must send UPI interrupts to the other IPs in the partition.
//          They will have no idea where the Level 0 BDT is, and cannot properly handle the interrupt.
//          So - the invoking processor stores the absolute address of the level 0 BDT in the mail slots
//          for the various IPs, and the UPI handler code in the IP reads that, and sets the Level 0 BDT
//          register accordingly before raising the Initial (class 30) interrupt.


/**
 * Class which implements the functionality necessary for an architecturally defined system control facility.
 * Our design stipulates the existence of exactly one of these in a proper configuration.
 * This object manages all console communication, and implements the system-wide dayclock.
 * It is also responsible for creating and managing the partition data bank which is used by the operating system.
 */
@SuppressWarnings("Duplicates")
public class SystemProcessor extends Processor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  HTTP listener
    //  All requests must use basic authentication for every message (in the headers, of course)
    //  All requests must include a (supposedly) unique UUID as a client identifier in the headers "Client={uuid}"
    //  This unique UUID must be used for every message sent by a given instance of a client.
    //  ----------------------------------------------------------------------------------------------------------------------------
    private class Listener extends SecureServer {

        private static final long AGE_OUT_MSECS = 10 * 60 * 1000;   //  10 minutes of no polling ages out a client
        private static final long POLL_WAIT_MSECS = 10000;          //  10 second poll delay

        /**
         * ClientInfo - information regarding a particular client
         */
        private class ClientInfo {
            private long _lastLogIdentifier = 0;
            private boolean _updatesReady = true;
            private long _lastPoll = System.currentTimeMillis();
        }

        /**
         * Invalid path handler class
         */
        private class DefaultRequestHandler implements HttpHandler {
            @Override
            public void handle(
                final HttpExchange exchange
            ) throws IOException {
                if (!validateCredentials(exchange)) {
                    return;
                }

                ClientInfo clientInfo = findClient(exchange);
                if (clientInfo == null) {
                    return;
                }

                String response = "Path or object does not exist";
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        /**
         * Handles requests against the /jumpkeys path
         */
        private class JumpKeysRequestHandler implements HttpHandler {
            @Override
            public void handle(
                final HttpExchange exchange
            ) throws IOException {
                if (!validateCredentials(exchange)) {
                    return;
                }

                ClientInfo clientInfo = findClient(exchange);
                if (clientInfo == null) {
                    return;
                }

                int code = HttpURLConnection.HTTP_OK;
                String response = "";
                if (exchange.getRequestMethod().equals(HttpMethod.PUT._value)
                    || exchange.getRequestMethod().equals(HttpMethod.PUT._value)) {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        SystemProcessorJumpKeys content = mapper.readValue(exchange.getRequestBody(),
                                                                           new TypeReference<SystemProcessorJumpKeys>() { });
                        long workingValue = _jumpKeys;
                        if (content._compositeValue != null) {
                            if ((content._compositeValue < 0) || (content._compositeValue > 0_777777_777777L)) {
                                throw new Exception("Invalid composite value");
                            }

                            workingValue = content._compositeValue;
                        }

                        if (content._componentValues != null) {
                            for (Map.Entry<String, Boolean> entry : content._componentValues.entrySet()) {
                                int jk = Integer.parseInt(entry.getKey());
                                if ((jk < 1) || (jk > 36)) {
                                    throw new Exception("Jump key out of range");
                                } else if (entry.getValue() == null) {
                                    throw new Exception("Value for jump key was unspecified");
                                }

                                long mask = 1L << (36 - jk);
                                if (entry.getValue()) {
                                    workingValue |= mask;
                                    LOGGER.info(String.format("Setting JK %d", jk));//TODO
                                } else {
                                    workingValue &= (mask ^ 0_777777_777777L);
                                    LOGGER.info(String.format("Clearing JK %d", jk));//TODO
                                }
                            }
                        }

                        _jumpKeys = workingValue;
                        _listener.updatePendingClients();
                    } catch (NumberFormatException ex) {
                        code = HttpURLConnection.HTTP_BAD_REQUEST;
                        response = "Jump key was not an integer";
                    } catch (Exception ex) {
                        code = HttpURLConnection.HTTP_BAD_REQUEST;
                        response = ex.getMessage();
                    }
                } else {
                    code = HttpURLConnection.HTTP_BAD_METHOD;
                }

                exchange.sendResponseHeaders(code, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }

        /**
         * Handle a poll request (a GET to /poll).
         * If we have a client ClientInfo record, send everything we have.
         * Otherwise check to see if there is anything new.  If so, send it.
         * Other-otherwise, wait for some period of time to see whether anything new pops up.
         */
        private class PollRequestHandler implements HttpHandler {
            @Override
            public void handle(
                final HttpExchange exchange
            ) throws IOException {
                if (!validateCredentials(exchange)) {
                    return;
                }

                ClientInfo clientInfo = findClient(exchange);
                if (clientInfo == null) {
                    return;
                }

                String method = exchange.getRequestMethod();
                if (!method.equals(HttpMethod.GET._value)) {
                    String content = method + " not allowed";
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, content.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(content.getBytes());
                    os.close();
                    return;
                }

                PollThread pthread = new PollThread(exchange,clientInfo);
                pthread.start();
            }
        }

        /**
         * Async thread for special handling for the /poll endpoint...
         * Needed so we can go back and service other requests while we're waiting on anything
         * noteworthy to occur.
         */
        private class PollThread extends Thread {

            private final ClientInfo _clientInfo;
            private final HttpExchange _exchange;

            private PollThread(
                final HttpExchange exchange,
                final ClientInfo clientInfo
            ) {
                _clientInfo = clientInfo;
                _exchange = exchange;
            }

            public void run() {
                _clientInfo._lastPoll = System.currentTimeMillis();
                synchronized (_clientInfo) {
                    if (!_clientInfo._updatesReady) {
                        try {
                            _clientInfo.wait(POLL_WAIT_MSECS);
                        } catch (InterruptedException ex) {
                            LOGGER.catching(ex);
                        }
                    }
                }

                SystemProcessorPoll content = new SystemProcessorPoll();
                if (_clientInfo._updatesReady) {
                    _clientInfo._updatesReady = false;

                    //TODO pull version information from somewhere reliable
                    content._identifiers = new SystemProcessorPoll.Identifiers();
                    content._identifiers._identifier = "Komodo System Processor Interface";
                    content._identifiers._copyright = "Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved";
                    content._identifiers._majorVersion = 1;
                    content._identifiers._minorVersion = 0;
                    content._identifiers._patch = 0;
                    content._identifiers._buildNumber = 0;
                    content._identifiers._versionString = "1.0.0.0";
                    content._identifiers._systemIdentifier = _systemIdentifier;

                    content._jumpKeys = _jumpKeys;

                    List<SystemProcessorPoll.HardwareLogEntry> logList = new LinkedList<>();
                    KomodoAppender.LogEntry[] logEntries = _appender.retrieveFrom(_clientInfo._lastLogIdentifier + 1);
                    for (KomodoAppender.LogEntry appenderEntry : logEntries) {
                        SystemProcessorPoll.HardwareLogEntry hlEntry = new SystemProcessorPoll.HardwareLogEntry();
                        hlEntry._timestamp = appenderEntry._timeMillis;
                        hlEntry._entity = appenderEntry._source;
                        hlEntry._message = appenderEntry._message;
                        _clientInfo._lastLogIdentifier = appenderEntry._identifier;
                        logList.add(hlEntry);
                    }
                    content._logEntries = logList.toArray(new SystemProcessorPoll.HardwareLogEntry[0]);

                    //TODO console info

                    //TODO system configuration info
                }

                try {
                    ObjectMapper mapper = new ObjectMapper();
                    String response = mapper.writeValueAsString(content);
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
         * Handle puts and posts to /session
         */
        private class SessionRequestHandler implements HttpHandler {
            @Override
            public void handle(
                final HttpExchange exchange
            ) throws IOException {
                if (!validateCredentials(exchange)) {
                    return;
                }

                String method = exchange.getRequestMethod();
                if (!method.equals(HttpMethod.POST._value) && !method.equals(HttpMethod.PUT._value)) {
                    String content = method + " not allowed";
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_METHOD, content.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(content.getBytes());
                    os.close();
                    return;
                }

                String clientId = UUID.randomUUID().toString();
                synchronized (_clientInfos) {
                    _clientInfos.put(clientId, new ClientInfo());
                }

                String content = new ObjectMapper().writeValueAsString(clientId);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, content.length());
                OutputStream os = exchange.getResponseBody();
                os.write(content.getBytes());
                os.close();
            }
        }

        /**
         * Data items
         */
        private Map<String, ClientInfo> _clientInfos = new HashMap<>();

        /**
         * constructor
         */
        private Listener(
            final int portNumber
        ) {
            super("SystemProcessor", portNumber);
        }

        /**
         * Client wants us to age-out any old client info objects
         */
        private void prune() {
            synchronized (_clientInfos) {
                long now = System.currentTimeMillis();
                Iterator iter = _clientInfos.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry entry = (Map.Entry) iter.next();
                    ClientInfo cinfo = (ClientInfo) entry.getValue();
                    if (now > (cinfo._lastPoll + AGE_OUT_MSECS)) {
                        iter.remove();
                    }
                }
            }
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
            appendHandler("/session", new SessionRequestHandler());
            appendHandler("/poll", new PollRequestHandler());
            start();
        }

        /**
         * Owner wants us to stop accepting requests
         */
        @Override
        public void stop() {
            super.stop();

            //  kill any pending polls
            Set<ClientInfo> cinfos;
            synchronized (this) {
                cinfos = new HashSet<>(_clientInfos.values());
                _clientInfos.clear();
            }

            for (ClientInfo cinfo : cinfos) {
                synchronized (cinfo) {
                    cinfo.notify();
                }
            }
        }

        /**
         * Notify any pending clients that they have updated info for polling
         */
        private void updatePendingClients() {
            Set<ClientInfo> cinfos;
            synchronized (this) {
                cinfos = new HashSet<>(_clientInfos.values());
            }

            for (ClientInfo cinfo : cinfos) {
                synchronized (cinfo) {
                    cinfo._updatesReady = true;
                    cinfo.notify();
                }
            }
        }

        /**
         * Checks the headers for a client id, then locates the corresponding ClientInfo object.
         * Returns null if ClientInfo object is not found or unspecified, in which case the response
         * is already filled in and closed - caller needs only return.
         */
        private ClientInfo findClient(
            final HttpExchange exchange
        ) throws IOException {
            List<String> values = exchange.getRequestHeaders().get("Client");
            if ((values != null) && (values.size() == 1)) {
                String clientId = values.get(0);
                synchronized (this) {
                    ClientInfo clientInfo = _clientInfos.get(clientId);
                    if (clientInfo != null) {
                        return clientInfo;
                    }
                }
            }

            String msg = "Missing or invalid client identifier\n";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(msg.getBytes());
            os.close();
            return null;
        }

        /**
         * Validate the credentials in the header of the given exchange object.
         * On failure, error status is posted to the HttpExchange which is then closed.
         * In this case, the client simply returns - all the work is done.
         *
         * @return true if credentials are valid, else false
         */
        private boolean validateCredentials(
            final HttpExchange exchange
        ) throws IOException {
            Headers headers = exchange.getRequestHeaders();

            List<String> values = headers.get("Authorization");
            if ((values != null) && (values.size() == 1)) {
                String[] split = values.get(0).split(" ");
                if ((split.length >= 2) && (_credentials.equals(split[1]))) {
                    return true;
                }
            }

            String msg = "Please enter credentials\n";
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, 0);
            OutputStream os = exchange.getResponseBody();
            os.write(msg.getBytes());
            os.close();
            return false;
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final long LOG_PERIODICITY_MSECS = 1000;             //  check the log every 1 second
    private static final long PRUNE_PERIODICITY_MSECS = 60 * 1000;      //  prune every 60 seconds

    private static final Logger LOGGER = LogManager.getLogger(SystemProcessor.class);
    private static SystemProcessor _instance = null;

    private KomodoAppender _appender;
    private String _credentials = "YWRtaW46YWRtaW4=";   //  TODO for now, it's admin/admin - later, pull from configuration
    private long _dayclockComparatorMicros;             //  value compared against emulator time to decide whether to cause interrupt
    private long _dayclockOffsetMicros = 0;             //  value applied to host system time in micros, to obtain emulator time
    private Listener _listener = null;
    private long _mostRecentLogIdentifier = 0;
    private long _jumpKeys = 0;
    private String _systemIdentifier = "TEST";          //  TODO pull from configuration


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param name node name of the SP
     */
    SystemProcessor(
        final String name,
        final int portNumber
    ) {
        super(Type.SystemProcessor, name, InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
        synchronized (SystemProcessor.class) {
            if (_instance != null) {
                LOGGER.error("Attempted to instantiate more than one SystemProcessor");
                assert (false);
            }
            _instance = this;
        }

        _listener = new Listener(portNumber);

        _appender = KomodoAppender.create();
        LoggerContext logContext = (LoggerContext) LogManager.getContext(false);
        Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.ALL);
        logContext.updateLoggers();
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

//    /**
//     * Establishes and populates a communications area in one of the configured MSPs.
//     * Should be invoked after clearing the various processors and before IPL.
//     * The format of the communications area is as follows:
//     *      +----------+----------+----------+----------+----------+----------+
//     * +0   |          |          |          |            #Entries            |
//     *      +----------+----------+----------+----------+----------+----------+
//     *      |                           First Entry                           |
//     * +1   |  SOURCE  |   DEST   |          |          |          |          |
//     *      +----------+----------+----------+----------+----------+----------+
//     * +2   |                           First Entry                           |
//     * +3   |       Area for communications from source to destination        |
//     *      +----------+----------+----------+----------+----------+----------+
//     *      |                       Subsequent Entries                        |
//     *      |                               ...                               |
//     *      +----------+----------+----------+----------+----------+----------+
//     * #ENTRIES:  Number of 3-word entries in the table.
//     * SOURCE:    UPI Index of processor sending the interrupt
//     * DEST:      UPI Index of processor to which the interrupt is sent
//     *
//     * It should be noted that not every combination of UPI index pairs are necessary,
//     * as not all possible paths between types of processors are supported, or implemented.
//     * Specifically, we allow interrupts from SPs and IPs to IOPs, as well as the reverse,
//     * and we allow interrupts from SPs to IPs and the reverse.
//     */
//    //TODO should this whole area just be part of the partition data bank?
//    private void establishCommunicationsArea(
//        final MainStorageProcessor msp,
//        final int segment,
//        final int offset
//    ) throws AddressingExceptionInterrupt {
//        //  How many communications slots do we need to create?
//        List<Processor> processors = InventoryManager.getInstance().getProcessors();
//
//        int iopCount = 0;
//        int ipCount = 0;
//        int spCount = 0;
//
//        for (Processor processor : processors) {
//            switch (processor._Type) {
//                case InputOutputProcessor:
//                    iopCount++;
//                    break;
//                case InstructionProcessor:
//                    ipCount++;
//                    break;
//                case SystemProcessor:
//                    spCount++;
//                    break;
//            }
//        }
//
//        //  slots from IPs and SPs to IOPs, and back
//        int entries = 2 * (ipCount + spCount) * iopCount;
//
//        //  slots from SPs to IPs
//        entries += 2 * spCount * ipCount;
//
//        int size = 1 + (3 * entries);
//        ArraySlice commsArea = new ArraySlice(msp.getStorage(segment), offset, size);
//        commsArea.clear();
//
//        commsArea.set(0, entries);
//        int ax = 1;
//
//        for (Processor source : processors) {
//            if ((source._Type == Type.InstructionProcessor)
//                || (source._Type == Type.SystemProcessor)) {
//                for (Processor destination : processors) {
//                    if (destination._Type == Type.InputOutputProcessor) {
//                        Word36 w = new Word36();
//                        w.setS1(source._upiIndex);
//                        w.setS2(destination._upiIndex);
//                        commsArea.set(ax, w.getW());
//                        ax += 3;
//
//                        w.setS1(destination._upiIndex);
//                        w.setS2(source._upiIndex);
//                        commsArea.set(ax, w.getW());
//                        ax += 3;
//                    } else if ((source._Type == Type.SystemProcessor)
//                               && (destination._Type == Type.InstructionProcessor)) {
//                        Word36 w = new Word36();
//                        w.setS1(source._upiIndex);
//                        w.setS2(destination._upiIndex);
//                        commsArea.set(ax, w.getW());
//                        ax += 3;
//
//                        w.setS1(destination._upiIndex);
//                        w.setS2(source._upiIndex);
//                        commsArea.set(ax, w.getW());
//                        ax += 3;
//                    }
//                }
//            }
//        }
//    }


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
    //  Async worker thread
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        LOGGER.info(_name + " worker thread starting");
        try {
            _listener.setup();
        } catch (Exception ex) {
            LOGGER.catching(ex);
            System.out.println("Caught " + ex.getMessage());
            System.out.println("Cannot start SystemProcessor secure server");
        } catch (Throwable t) {
            System.out.println("Caught " + t.getMessage());
        }

        long nextLogCheck = System.currentTimeMillis() + LOG_PERIODICITY_MSECS;
        long nextPrune = System.currentTimeMillis() + PRUNE_PERIODICITY_MSECS;
        while (!_workerTerminate) {
            long now = System.currentTimeMillis();
            if (now > nextLogCheck) {
                if (_appender.getMostRecentIdentifier() > _mostRecentLogIdentifier) {
                    _listener.updatePendingClients();
                    _mostRecentLogIdentifier = _appender.getMostRecentIdentifier();
                }
                nextLogCheck += LOG_PERIODICITY_MSECS;
            }

            if (now > nextPrune) {
                _listener.prune();
                nextPrune += PRUNE_PERIODICITY_MSECS;
            }

            //  Check UPI ACKs and SENDs
            //  ACKs mean we can send another IO
            boolean didSomething = false;
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

            if (!didSomething) {
                try {
                    synchronized (this) {
                        wait(25);
                    }
                } catch (InterruptedException ex) {
                    LOGGER.catching(ex);
                }
            }
        }

        _listener.stop();
        LOGGER.info(_name + " worker thread terminating");
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

    /**
     * Resets the conceptual system console
     */
    void consoleReset() {
        //TODO
    }

    /**
     * Sends a read-only message to the conceptual system console.
     * The message may be padded or truncated to an appropriate size.
     * @param messageIdentifier unique identifier of this message among all messages
     * @param message actual message to be sent
     */
    void consoleSendReadOnlyMessage(
        final long messageIdentifier,
        final String message
    ) {
        //TODO
    }

    /**
     * Sends a read-reply message to the conceptual system console.
     * The message may be padded or truncated to an appropriate size.
     * The eventual reply is guaranteed not to exceed the indicated max characters size.
     * The message will be identified with a console message index, which is returned from this call.
     * @param messageIdentifier unique identifier of this message among all messages
     * @param message actual message to be sent
     * @param replyMaxCharacters max characters allowed in the response
     */
    int consoleSendReadReplyMessage(
        final long messageIdentifier,
        final String message,
        final int replyMaxCharacters
    ) {
        return 0;//TODO
    }

    /**
     * Sends a pair of status messages to the conceptual system console.
     * The messages may be padded or truncated to an appropriate size.
     */
    void consoleSendStatusMessage(
        final String message1,
        final String message2
    ) {
        //TODO
    }

    /**
     * Retrieves the master dayclock time in microseconds since epoch.
     * This time is based on the host system time, offset by a value to allow the emulated system time
     * to differ from the host system.
     */
    long dayclockGetMicros() {
        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        return systemMicros + _dayclockOffsetMicros;
    }

    /**
     * Sets the system comparator value which drives dayclock interrupts when they're enabled.
     * This value should always be compared against the value returned by dayclockGetMicros() which
     * applies the system offset - the comparator value is always assumed to be offset by the same amount.
     * BTW: We don't actually do any interrupt instigation, nor care if they're enabled - that is handled by the IPs.
     */
    void dayclockSetComparatorMicros(
        final long value
    ) {
        _dayclockComparatorMicros = value;
    }

    /**
     * Stores the difference between the requested dayclock time in microseconds, and the actual host system time
     * converted to dayclock microseconds.  Subsequent dayclock reads must apply this offset.
     */
    void dayclockSetMicros(
        final long value
    ) {
        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        _dayclockOffsetMicros = value - systemMicros;
    }

    /**
     * Retrieve 36-bit word representing the jump keys.
     * JK1 is in the MSBit of 36 bits, JK36 is in the LSbit
     */
    long jumpKeysGet() {
        return _jumpKeys;
    }

    /**
     * Sets the set of jump keys.
     * @param value JK1 is in the MSBit of 36 bits, JK36 is in the LSbit
     */
    void jumpKeysSet(
        final long value
    ) {
        _jumpKeys = value;
        _listener.updatePendingClients();
    }
}
