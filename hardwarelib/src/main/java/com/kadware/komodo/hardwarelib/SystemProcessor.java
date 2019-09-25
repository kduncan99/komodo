/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.SecureServer;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    private SecureServer _httpServer = null;
    private long _jumpKeys = 0;
    private String _credentials = "YWRtaW46YWRtaW4=";   //  TODO for now, it's admin/admin - later, pull from configuration
    private String _systemIdentifier = "TEST";          //  TODO pull from configuration

    /**
     * Handles requests against the console path
     * GET /console
     *      provides 24 lines of text to be displayed upon the console
     *          First two lines are always status (but may be blank)
     *          {digit} '-' {message}  is read-reply
     *          {sp} {sp} {message} is read-only
     *          {sp} {message} is input
     *      We can use the above pattens to choose the color for the various displays, if we wish to do so
     * POST /console
     *      Sends input to the console as such:
     *        {message}
     *        {digit} {sp} {message}
     */
    private class ConsoleRequestHandler implements HttpHandler {
        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            String response = "Not Ready.\n";
            exchange.sendResponseHeaders(404, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    /**
     * Handles everything which isn't one of the accepted paths
     */
    private class DefaultRequestHandler implements HttpHandler {
        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            if (!validateCredentials(exchange)) {
                return;
            }

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("Identifier", "Komodo System Processor Interface");
            response.put("Copyright", "Copyright (c) 2019 by Kurt Duncan All Rights Reserved");
            response.put("MajorVersion", new Integer(1));
            response.put("MinorVersion", new Integer(0));
            response.put("Patch", new Integer(0));
            response.put("Build", new Integer(0));
            response.put("VersionString", "1.0.0.0");
            response.put("SystemIdentifier", _systemIdentifier);

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(response);
            exchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, json.length());
            OutputStream os = exchange.getResponseBody();
            os.write(json.getBytes());
            os.close();
        }
    }

    /**
     * Handles requests against the jumpkeys path
     * GET /jumpkeys
     *      Produces a JSON string containing a 12-digit octal value representing the current state of the jump keys
     * PUT /jumpkeys {restObject}
     *      For the rest object, the key is a string representation of an integer from 1 to 36,
     *      and the value is a boolean indicating whether the corresponding jump key should be on (true) or off (false)
     */
    private class JumpKeysRequestHandler implements HttpHandler {
        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            if (!validateCredentials(exchange)) {
                return;
            }

            String[] pathSplit = exchange.getRequestURI().getPath().split("/");
            int code = HttpURLConnection.HTTP_OK;
            String response = "Done\n";
            switch (exchange.getRequestMethod()) {
                case "GET":
                    response = String.format("\"%012o\"\n", _jumpKeys);
                    break;

                case "PUT": {
                    try {
                        long workingValue = _jumpKeys;
                        Map<String, Object> jsonObject = parseBody(exchange);
                        for (Map.Entry<String, Object> entry : jsonObject.entrySet()) {
                            int jk = Integer.parseInt(entry.getKey());
                            if ((jk < 1) || (jk > 36)) {
                                throw new NumberFormatException();
                            }

                            if (!(entry.getValue() instanceof Boolean)) {
                                response = "Invalid value\n";
                                code = HttpURLConnection.HTTP_BAD_REQUEST;
                                break;
                            }

                            boolean setting = (Boolean) entry.getValue();
                            long mask = 1L << (36 - jk);
                            if (setting) {
                                workingValue |= mask;
                            } else {
                                workingValue &= (mask ^ 0_777777_777777L);
                            }
                        }

                        if (code == HttpURLConnection.HTTP_OK) {
                            _jumpKeys = workingValue;
                        }
                    } catch (NumberFormatException ex) {
                        response = "Invalid jump key";
                        code = HttpURLConnection.HTTP_BAD_REQUEST;
                    } catch (IOException ex) {
                        response = ex.getMessage() + "\n";
                        code = HttpURLConnection.HTTP_INTERNAL_ERROR;
                    }
                    break;
                }

                default:
                    response = "Not Allowed";
                    code = HttpURLConnection.HTTP_BAD_REQUEST;
            }

            exchange.sendResponseHeaders(code, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private static class LogsRequestHandler implements HttpHandler {
        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            String response = "Throwing another log on the fire.\n";
            exchange.sendResponseHeaders(404, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private class SystemRequestHandler implements HttpHandler {
        @Override
        public void handle(
            final HttpExchange exchange
        ) throws IOException {
            String response = "Not Ready.\n";
            exchange.sendResponseHeaders(404, response.length());
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
        _httpServer = new SecureServer(name, port);
        synchronized (SystemProcessor.class) {
            if (_instance != null) {
                LOGGER.error("Attempted to instantiate more than one SystemProcessor");
                assert (false);
            }
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

    /**
     * Quick wrapper to convert input stream representing a JSON object into a generic Map
     */
    private Map<String, Object> parseBody(
        final HttpExchange exchange
    ) throws IOException {
        return new ObjectMapper().readValue(exchange.getRequestBody(), new TypeReference<Map<String, Object>>(){ });
    }

    /**
     * Validate the credentials in the header of the given exchange object.
     * If okay, return true.  If not, send back the appropriate response and return false.
     */
    private boolean validateCredentials(
        final HttpExchange exchange
    ) throws IOException {
        Headers headers = exchange.getRequestHeaders();
        List<String> values = headers.get("Authorization");
        if ((values != null) && (values.size() > 0)) {
            String[] split = values.get(0).split(" ");
            if ((split.length >= 2) && (_credentials.equals(split[1]))) {
                return true;
            }
        }

        String msg = "Please enter credentials:\n";
        exchange.sendResponseHeaders(HttpURLConnection.HTTP_UNAUTHORIZED, 0);
        OutputStream os = exchange.getResponseBody();
        os.write(msg.getBytes());
        os.close();

        return false;
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
        try {
            _httpServer.setup();
            _httpServer.appendHandler("/console", new ConsoleRequestHandler());
            _httpServer.appendHandler("/jumpkeys", new JumpKeysRequestHandler());
            _httpServer.appendHandler("/logs", new LogsRequestHandler());
            _httpServer.appendHandler("/system", new SystemRequestHandler());
            _httpServer.appendHandler("/", new DefaultRequestHandler());
            _httpServer.start();
        } catch (Exception ex) {
            LOGGER.catching(ex);
            System.out.println("Caught " + ex.getMessage());
            System.out.println("Cannot start SystemProcessor secure server");
        } catch (Throwable t) {
            System.out.println("Caught " + t.getMessage());
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
        _httpServer.stop();
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
    }
}
