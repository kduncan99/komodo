/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.KomodoAppender;
import com.kadware.komodo.baselib.Word36;
import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoField;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;

//TODO move this commentary somewhere else, where it makes sense
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
public class SystemProcessor extends Processor implements JumpKeyPanel {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Data
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final long LOG_PERIODICITY_MSECS = 1000;             //  check the log every 1 second
    private static final String CONFIGURATION_FILENAME = "../resources/configuration.json";

    private static final Logger LOGGER = LogManager.getLogger(SystemProcessor.class);
    private static SystemProcessor _instance = null;

    private KomodoAppender _appender;                   //  Log appender, so we can catch log entries
    private Configurator _configurator;
    private long _dayclockComparatorMicros;             //  value compared against emulator time to decide whether to cause interrupt
    private long _dayclockOffsetMicros = 0;             //  value applied to host system time in micros, to obtain emulator time
    private long _mostRecentLogIdentifier = 0;
    private long _jumpKeys = 0;
    private final SystemConsole _console;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor
     * @param name node name of the SP
     */
    SystemProcessor (
        final String name
    ) {
        super(Type.SystemProcessor, name, InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
        synchronized (SystemProcessor.class) {
            if (_instance != null) {
                LOGGER.error("Attempted to instantiate more than one SystemProcessor");
                assert (false);
            }
            _instance = this;
        }

        _appender = KomodoAppender.create();
        LoggerContext logContext = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.ALL);
        logContext.updateLoggers();

        try {
            _configurator = Configurator.read(CONFIGURATION_FILENAME);
        } catch (IOException ex) {
            LOGGER.catching(ex);
            throw new RuntimeException("Cannot load configuration file");
        }

        _console = new RESTSystemConsole(name + "Console", _configurator, this);
    }

    /**
     * constructor for testing
     */
    public SystemProcessor() {
        super(Type.SystemProcessor, "SP0", InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
        _instance = this;
        _console = new RESTSystemConsole("SP0Console",
                                         _configurator,
                                         this);
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
    //  Implementations / overrides of JumpKeyPanel
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public boolean getJumpKey(
        final int jumpKeyId
    ) {
        if ((jumpKeyId > 0) && (jumpKeyId < 37)) {
            long mask = 1 << (36 - jumpKeyId);
            return (_jumpKeys & mask) != 0;
        }
        return false;
    }

    @Override
    public Word36 getJumpKeys() {
        return new Word36(_jumpKeys);
    }

    @Override
    public void setJumpKey(
        final int jumpKeyId,
        final boolean value
    ) {
        if ((jumpKeyId > 0) && (jumpKeyId < 37)) {
            if (value) {
                long mask = 1 << (36 - jumpKeyId);
                _jumpKeys |= mask;
            } else {
                long mask = 1 << (36 - jumpKeyId);
                mask ^= 0_777777_777777L;
                _jumpKeys &= mask;
            }
        }
    }

    @Override
    public void setJumpKeys(
        final Word36 word36
    ) {
        _jumpKeys = word36.getW();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Async worker thread for the SystemProcessor
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        LOGGER.info(_name + " worker thread starting");

        _console.start();
        long nextLogCheck = System.currentTimeMillis() + LOG_PERIODICITY_MSECS;
        while (!_workerTerminate) {
            long now = System.currentTimeMillis();
            if (now > nextLogCheck) {
                if (_appender.getMostRecentIdentifier() > _mostRecentLogIdentifier) {
                    KomodoAppender.LogEntry[] appenderEntries = _appender.retrieveFrom(_mostRecentLogIdentifier + 1);
                    if (appenderEntries.length > 0) {
                        _console.postSystemLogEntries(appenderEntries);
                        _mostRecentLogIdentifier = _appender.getMostRecentIdentifier();
                    }
                }
                nextLogCheck += LOG_PERIODICITY_MSECS;
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

        _console.stop();
        LOGGER.info(_name + " worker thread terminating");
    }


    //  ------------------------------------------------------------------------
    //  Public methods - for other processors to invoke
    //  Mostly for InstructionProcessor's SYSC instruction
    //  ------------------------------------------------------------------------

    String consolePollInputMessage() {
        SystemConsole.InputMessage im = _console.pollInputMessage();
        if (im == null) {
            return null;
        } else {
            return im._text;
        }
    }

    /**
     * Resets the conceptual system console
     */
    void consoleReset() {
        _console.reset();
    }

    /**
     * Sends an output message to the conceptual system console.
     * The message may be padded or truncated to an appropriate size.
     * It may be generated by the OS via the SYSC instruction.
     * @param pinned true if this is a message which should not scroll off (e.g., a Read-Reply message)
     * @param message actual message to be sent
     */
    void consoleSendOutputMessage(
        final boolean pinned,
        final String message
    ) {
        SystemConsole.OutputMessage msg = new SystemConsole.OutputMessage(_console.getNextMessageIdentifier(), pinned, message);
        _console.postOutputMessage(msg);
    }

    /**
     * Sends a list of status messages to the conceptual system console.
     * The messages may be padded or truncated to an appropriate size.
     * Functions by accepting the message sourced from the OS, and queueing it for eventual delivery via poll response.
     * It may be generated by the OS via the SYSC instruction.
     */
    void consoleSendStatusMessage(
        final String[] messages
    ) {
        SystemConsole.StatusMessage msg = new SystemConsole.StatusMessage(_console.getNextMessageIdentifier(), messages);
        _console.postStatusMessage(msg);
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
}
