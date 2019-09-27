/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                ClientInfo clientInfo = validateCredentials(exchange);
                if (clientInfo != null) {
                    String response = "Path or object does not exist";
                    exchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                }
            }
        }

        /**
         * Handle a poll request.
         * If we have a client ClientInfo record, send everything we have.
         * Otherwise check to see if there is anything new.  If so, send it.
         * Other-otherwise, wait for some period of time to see whether anything new pops up.
         */
        private class PollRequestHandler implements HttpHandler {
            @Override
            public void handle(
                final HttpExchange exchange
            ) throws IOException {
                ClientInfo clientInfo = validateCredentials(exchange);
                if (clientInfo == null) { return; }

                clientInfo._lastPoll = System.currentTimeMillis();
                if (!clientInfo._updatesReady) {
                    synchronized (clientInfo) {
                        try {
                            clientInfo.wait(POLL_WAIT_MSECS);
                        } catch (InterruptedException ex) {
                            LOGGER.catching(ex);
                        }
                    }
                }

                SystemProcessorPoll content = new SystemProcessorPoll();
                if (clientInfo._updatesReady) {
                    clientInfo._updatesReady = false;

                    //TODO pull version information from somewhere reliable
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
                    KomodoAppender.LogEntry[] logEntries = _appender.retrieveFrom(clientInfo._lastLogIdentifier + 1);
                    for (KomodoAppender.LogEntry appenderEntry : logEntries) {
                        SystemProcessorPoll.HardwareLogEntry hlEntry = new SystemProcessorPoll.HardwareLogEntry();
                        hlEntry._timestamp = appenderEntry._timeMillis;
                        hlEntry._entity = appenderEntry._source;
                        hlEntry._message = appenderEntry._message;
                        clientInfo._lastLogIdentifier = appenderEntry._identifier;
                        logList.add(hlEntry);
                    }
                    content._logEntries = logList.toArray(new SystemProcessorPoll.HardwareLogEntry[0]);

                    //TODO console info

                    //TODO system configuration info
                }

                ObjectMapper mapper = new ObjectMapper();
                String response = mapper.writeValueAsString(content);
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
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
         * Validate the credentials in the header of the given exchange object.
         * Also check for the client identifier - if it doesn't exist, complain.
         * If it is new, set up a client info object to keep track of the client.
         * If something is wrong, send the response and return null.
         * Otherwise, return the newly-created or located ClientInfo object
         */
        private ClientInfo validateCredentials(
            final HttpExchange exchange
        ) throws IOException {
            Headers headers = exchange.getRequestHeaders();

            boolean credentials = false;
            List<String> values = headers.get("Authorization");
            if ((values != null) && (values.size() == 1)) {
                String[] split = values.get(0).split(" ");
                if ((split.length >= 2) && (_credentials.equals(split[1]))) {
                    credentials = true;
                }
            }

            if (!credentials) {
                String msg = "Please enter credentials\n";
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(msg.getBytes());
                os.close();
                return null;
            }

            values = headers.get("Client");
            if ((values == null) || (values.size() != 1)) {
                String msg = "Missing client identifier\n";
                exchange.sendResponseHeaders(HttpURLConnection.HTTP_BAD_REQUEST, 0);
                OutputStream os = exchange.getResponseBody();
                os.write(msg.getBytes());
                os.close();
                return null;
            }

            String clientId = values.get(0);
            ClientInfo clientInfo;
            synchronized (this) {
                clientInfo = _clientInfos.get(clientId);
                if (clientInfo == null) {
                    clientInfo = new ClientInfo();
                    _clientInfos.put(clientId, clientInfo);
                }
            }

            return clientInfo;
        }
    }

//    /**
//     * Handles requests against the jumpkeys path
//     */
//    private class JumpKeysRequestHandler implements HttpHandler {
//        @Override
//        public void handle(
//            final HttpExchange exchange
//        ) throws IOException {
//            if (!validateCredentials(exchange)) {
//                return;
//            }
//
//            int code = HttpURLConnection.HTTP_OK;
//            String response = "";
//            if (exchange.getRequestMethod().equals(HttpMethod.GET._value)) {
//                SystemProcessorJumpKeys content = new SystemProcessorJumpKeys();
//                content._compositeValue = _jumpKeys;
//                long mask = 0_400000_000000L;
//                for (int jk = 1; jk <= 36; ++jk) {
//                    String key = String.format("%d", jk);
//                    boolean value = (_jumpKeys & mask) != 0;
//                    content._componentValues.put(key, value);
//                }
//
//                ObjectMapper mapper = new ObjectMapper();
//                response = mapper.writeValueAsString(content);
//            } else if (exchange.getRequestMethod().equals(HttpMethod.PUT._value)) {
//                try {
//                    ObjectMapper mapper = new ObjectMapper();
//                    SystemProcessorJumpKeys content = mapper.readValue(exchange.getRequestBody(),
//                                                                       new TypeReference<SystemProcessorJumpKeys>() { });
//                    long workingValue = _jumpKeys;
//                    if (content._compositeValue != null) {
//                        if ((content._compositeValue < 0) || (content._compositeValue > 0_777777_777777L)) {
//                            throw new Exception("Invalid composite value");
//                        }
//
//                        workingValue = content._compositeValue;
//                    }
//
//                    if (content._componentValues != null) {
//                        for (Map.Entry<String, Boolean> entry : content._componentValues.entrySet()) {
//                            int jk = Integer.parseInt(entry.getKey());
//                            if ((jk < 1) || (jk > 36)) {
//                                throw new Exception("Jump key out of range");
//                            } else if (entry.getValue() == null) {
//                                throw new Exception("Value for jump key was unspecified");
//                            }
//
//                            long mask = 1L << (36 - jk);
//                            if (entry.getValue()) {
//                                workingValue |= mask;
//                            } else {
//                                workingValue &= (mask ^ 0_777777_777777L);
//                            }
//                        }
//                    }
//                } catch (NumberFormatException ex) {
//                    code = HttpURLConnection.HTTP_BAD_REQUEST;
//                    response = "Jump key was not an integer";
//                } catch (Exception ex) {
//                    code = HttpURLConnection.HTTP_BAD_REQUEST;
//                    response = ex.getMessage();
//                }
//            } else {
//                code = HttpURLConnection.HTTP_BAD_METHOD;
//            }
//
//            exchange.sendResponseHeaders(code, response.length());
//            OutputStream os = exchange.getResponseBody();
//            os.write(response.getBytes());
//            os.close();
//        }
//    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final long LOG_PERIODICITY_MSECS = 1000;             //  check the log every 1 second
    private static final long PRUNE_PERIODICITY_MSECS = 60 * 1000;      //  prune every 60 seconds

    private static final Logger LOGGER = LogManager.getLogger(SystemProcessor.class);
    private static SystemProcessor _instance = null;

    private KomodoAppender _appender;
    private String _credentials = "YWRtaW46YWRtaW4=";   //  TODO for now, it's admin/admin - later, pull from configuration
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
