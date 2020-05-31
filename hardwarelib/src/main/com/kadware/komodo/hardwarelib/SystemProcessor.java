/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Credentials;
import com.kadware.komodo.baselib.KomodoLoggingAppender;
import com.kadware.komodo.baselib.VirtualAddress;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.baselib.exceptions.BinaryLoadException;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.komodo.hardwarelib.interrupts.MachineInterrupt;
import com.kadware.komodo.kex.klink.LoadableBank;
import java.io.BufferedWriter;
import java.time.Instant;
import java.time.temporal.ChronoField;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.message.EntryMessage;

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

    private static final Logger LOGGER = LogManager.getLogger(SystemProcessor.class.getSimpleName());

    private KomodoLoggingAppender _appender;            //  Log appender, so we can catch log entries
    private SystemProcessorInterface _systemConsoleInterface;
    Credentials _credentials;                           //  Current admin credentials for logging into the SPIF
    private long _dayclockComparatorMicros;             //  compared against emulator time to decide whether to cause interrupt
    private long _dayclockOffsetMicros = 0;             //  applied to host system time in micros, to obtain emulator time
    private Integer _httpPort = null;
    private Integer _httpsPort = null;
    private long _jumpKeys = 0;
    private long _mostRecentLogIdentifier = 0;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * constructor for SPs with an HTTPSystemProcessorInterface (currently, that's all we have)
     * @param name node name of the SP
     * @param httpPort port number for HTTP interface - null if we don't want HTTP
     * @param httpsPort port number for HTTPS interface - null if we don't want HTTPS
     */
    SystemProcessor(
        final String name,
        final Integer httpPort,
        final Integer httpsPort,
        final Credentials credentials
    ) {
        super(ProcessorType.SystemProcessor, name, InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
        _httpPort = httpPort;
        _httpsPort = httpsPort;
        _credentials = credentials;

        _appender = KomodoLoggingAppender.create();
        LoggerContext logContext = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configurator.setAllLevels(LogManager.getRootLogger().getName(), Level.ALL);
        logContext.updateLoggers();
    }

    /**
     * constructor for testing
     */
    public SystemProcessor() {
        super(ProcessorType.SystemProcessor, "SP0", InventoryManager.FIRST_SYSTEM_PROCESSOR_UPI_INDEX);
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
//            if ((source._Type == ProcessorType.InstructionProcessor)
//                || (source._Type == ProcessorType.SystemProcessor)) {
//                for (Processor destination : processors) {
//                    if (destination._Type == ProcessorType.InputOutputProcessor) {
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
//                    } else if ((source._Type == ProcessorType.SystemProcessor)
//                               && (destination._Type == ProcessorType.InstructionProcessor)) {
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
            long mask = 1L << (36 - jumpKeyId);
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
            long mask = 1L << (36 - jumpKeyId);
            if (value) {
                _jumpKeys |= mask;
            } else {
                mask ^= 0_777777_777777L;
                _jumpKeys &= mask;
            }
            _systemConsoleInterface.jumpKeysUpdated();
        }
    }

    @Override
    public void setJumpKeys(
        final Word36 word36
    ) {
        _jumpKeys = word36.getW();
        _systemConsoleInterface.jumpKeysUpdated();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Async worker thread for the SystemProcessor
    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public void run() {
        _isRunning = true;
        LOGGER.info(_name + " worker thread starting");

        _systemConsoleInterface = new HTTPSystemProcessorInterface(this,
                                                                    _name + "-SPIF",
                                                                   _httpPort,
                                                                   _httpsPort);
        _systemConsoleInterface.start();

        _isReady = true;
        LOGGER.info(_systemConsoleInterface.getName() + " Ready");
        long nextLogCheck = System.currentTimeMillis() + LOG_PERIODICITY_MSECS;
        while (!_workerTerminate) {
            long now = System.currentTimeMillis();
            if (now > nextLogCheck) {
                if (_appender.getMostRecentIdentifier() > _mostRecentLogIdentifier) {
                    KomodoLoggingAppender.LogEntry[] appenderEntries =
                        _appender.retrieveFrom(_mostRecentLogIdentifier + 1);
                    if (appenderEntries.length > 0) {
                        _systemConsoleInterface.postSystemLogEntries(appenderEntries);
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

        _systemConsoleInterface.stop();
        _systemConsoleInterface = null;
        LOGGER.info(_name + " worker thread terminating");
        _isReady = false;
        _isRunning = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods - for other processors to invoke
    //  Mostly for InstructionProcessor's SYSC instruction
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Cancels a previously-sent read-reply message, and optionally replaces the previous message with new text
     */
    void consoleCancelReadReplyMessage(
        final int consoleId,
        final int messageId,
        final String replacementText
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(consoleId=%d messageId=%d text='%s')",
                                            this.getClass().getSimpleName(),
                                            "consoleCancelReadReplyMessage",
                                            consoleId,
                                            messageId,
                                            replacementText);
        _systemConsoleInterface.cancelReadReplyMessage(consoleId, messageId, replacementText);
    }

    /**
     * Reads input from the system console.
     * If no input is available, the return value is null.
     * If input is available, unsolicited input is returned with a single leading blank,
     * while responses to read-reply messages are returned in the format {n}{s} where {n} is the ASCII
     * representation of the message id followed by the text (if any).
     */
    SystemProcessorInterface.ConsoleInputMessage consolePollInputMessage(
        final long waitMilliseconds
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(waitMilliseconds=%d)",
                                            this.getClass().getSimpleName(),
                                            "consolePollInputMessage",
                                            waitMilliseconds);
        SystemProcessorInterface.ConsoleInputMessage result = _systemConsoleInterface.pollInputMessage(waitMilliseconds);
        LOGGER.traceExit(em, result);
        return result;
    }

    /**
     * Resets the conceptual system console
     */
    void consoleReset() {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "consoleReset");
        _systemConsoleInterface.reset();
        LOGGER.traceExit(em);
    }

    /**
     * Sends a read-only output message to the conceptual system console.
     * The message may be padded or truncated to an appropriate size.
     * It may be generated by the OS via the SYSC instruction.
     * @param message actual message to be sent
     * @param rightJustified true if this message is to appear right-justified
     * @param cached true to cache this for future new console sessions
     */
    void consoleSendReadOnlyMessage(
        final int consoleId,
        final String message,
        final Boolean rightJustified,
        final Boolean cached
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(consoleId=%d message='%s' rightJust=%s cached=%s)",
                                            this.getClass().getSimpleName(),
                                            "consoleSendReadOnlyMessage",
                                            consoleId,
                                            message,
                                            rightJustified,
                                            cached);
        _systemConsoleInterface.postReadOnlyMessage(consoleId, message, rightJustified, cached);
        LOGGER.traceExit(em);
    }

    /**
     * Convenience wrapper for the above...
     */
    void consoleSendReadOnlyMessage(
        final int consoleId,
        final String message
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(consoleId=%d message='%s')",
                                            this.getClass().getSimpleName(),
                                            "consoleSendReadOnlyMessage",
                                            consoleId,
                                            message);
        _systemConsoleInterface.postReadOnlyMessage(consoleId, message, false, true);
        LOGGER.traceExit(em);
    }

    /**
     * Wrapper for above, using absolute address
     */
    void consoleSendReadOnlyMessage(
        final int consoleId,
        final AbsoluteAddress messageAddress,
        final Boolean rightJustified,
        final Boolean cached
    ) {

    }

    /**
     * Sends a read-reply output message to the conceptual system console.
     * The message may be padded or truncated to an appropriate size.
     * It may be generated by the OS via the SYSC instruction.
     * @param message actual message to be sent
     */
    void consoleSendReadReplyMessage(
        final int consoleId,
        final int messageId,
        final String message,
        final int maxReplyLength
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(consoleId=%d messageId=%s message='%s' maxReplyLength=%d)",
                                            this.getClass().getSimpleName(),
                                            "consoleSendReadReplyMessage",
                                            consoleId,
                                            messageId,
                                            message,
                                            maxReplyLength);
        _systemConsoleInterface.postReadReplyMessage(consoleId, messageId, message, maxReplyLength);
        LOGGER.traceExit(em);
    }

    /**
     * Sends a list of status messages to the conceptual system console.
     * The messages may be padded or truncated to an appropriate size.
     * Functions by accepting the message sourced from the OS, and queueing it for eventual delivery via poll response.
     * It may be generated by the OS via the SYSC instruction.
     * Generally there will be exactly two - but implementing consoles should not rely on this
     */
    void consoleSendStatusMessage(
        final String[] messages
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(#messages=%d)",
                                            this.getClass().getSimpleName(),
                                            "consoleSendStatusMessage",
                                            messages.length);
        _systemConsoleInterface.postStatusMessages(messages);
        LOGGER.traceExit(em);
    }

    /**
     * Retrieves the master dayclock time in microseconds since epoch.
     * This time is based on the host system time, offset by a value to allow the emulated system time
     * to differ from the host system.
     */
    long dayclockGetMicros() {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "dayclockGetMicros");

        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        long result = systemMicros + _dayclockOffsetMicros;

        LOGGER.traceExit(em, result);
        return result;
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
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "dayclockSetComparatorMicros");

        _dayclockComparatorMicros = value;

        LOGGER.traceExit(em);
    }

    /**
     * Stores the difference between the requested dayclock time in microseconds, and the actual host system time
     * converted to dayclock microseconds.  Subsequent dayclock reads must apply this offset.
     */
    void dayclockSetMicros(
        final long value
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "dayclockSetMicros");

        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        _dayclockOffsetMicros = value - systemMicros;

        LOGGER.traceExit(em);
    }

    /**
     * Allocates a chunk of storage from the indicated main storage processor
     */
    AbsoluteAddress iplAllocate(
        final String moduleName,
        final MainStorageProcessor mainStorageProcessor,
        final int words,
        final boolean useFixedStorage
    ) throws AddressingExceptionInterrupt,
             BinaryLoadException {
        if (useFixedStorage) {
            //  Word 0 is a sentinel, word 1 is the next free offset, word 2 is the remaining space
            ArraySlice fixedSlice = mainStorageProcessor.getStorage(0);
            long sentinel = fixedSlice.get(0);
            int nextOffset = (int) fixedSlice.get(1);
            int remaining = (int) fixedSlice.get(2);

            if (sentinel != 0_777000_777000L) {
                throw new BinaryLoadException(moduleName, "Fixed arena not established");
            }

            if (words > remaining) {
                throw new BinaryLoadException(moduleName, "Out of fixed space");
            }

            AbsoluteAddress result = new AbsoluteAddress(mainStorageProcessor._upiIndex, 0, nextOffset);
            nextOffset += words;
            remaining -= words;
            fixedSlice.set(1, nextOffset);
            fixedSlice.set(2, remaining);

            return result;
        } else {
            int segIndex = mainStorageProcessor.createSegment(words);
            return new AbsoluteAddress(mainStorageProcessor._upiIndex, segIndex, 0);
        }
    }

    /**
     * Loads a single or multi-banked binary produced by the linker, into cleared memory.
     * Produces a level 0 bank descriptor table and any other necessary BDTs.
     * Sets up minimal interrupt handling, sufficient for IPL to succeed
     * - presumes the starting address is the first word of the first-defined bank
     * Sets up bank registers in the given IP to represent the bank descriptor tables
     * Sets up the designator register for ring 0, level 0, exec mode, etc
     * Sets the PAR,PC per the starting address (see above)
     * Optionally creates a configuration bank, and bases it on B2 for extended mode, or B13 for basic mode
     *      this bank is NOT in any bank descriptor table, so if you unbase it, you cannot get it back
     * Finally, sends an IPL to the indicated instruction processor to get things rolling.
     * @param moduleName name of the module being loaded
     * @param loadableBanks Represents the code to be loaded
     * @param mainStorageProcessorUPI UPI of the MSP to be loaded
     * @param instructionProcessorUPI UPI of the IP to be loaded
     * @param useFixedStorage true to use fixed MSP storage; false to use dynamically-allocated segments
     */
    void iplBinary(
        final String moduleName,
        final LoadableBank[] loadableBanks,
        final int mainStorageProcessorUPI,
        final int instructionProcessorUPI,
        final boolean useFixedStorage,
        final boolean createConfigBank
    ) throws BinaryLoadException,
             MachineInterrupt,
             UPINotAssignedException,
             UPIProcessorTypeException {
        EntryMessage em = LOGGER.traceEntry("iplLoadBinary(module=%s mspupi=%d ipupi=%d, fixed=%s config=%s",
                                            moduleName,
                                            mainStorageProcessorUPI,
                                            instructionProcessorUPI,
                                            useFixedStorage,
                                            createConfigBank);

        //  check BDIs - we need bank descriptor tables for level 0, and for all other levels indicated
        //  the vector indicates the highest BDI for each level, where the level is the index.
        int[] bdtVector = { 32, -1, -1, -1, -1, -1, -1, -1 };
        for (LoadableBank lb : loadableBanks) {
            if ((lb._bankLevel < 0) || (lb._bankLevel > 7)) {
                throw new BinaryLoadException(moduleName, "Invalid bank level");
            }

            if ( (lb._bankDescriptorIndex > 077777)
                || ((lb._bankLevel == 0) && (lb._bankDescriptorIndex < 31))
                || (lb._bankDescriptorIndex < 0) ) {
                throw new BinaryLoadException(moduleName, "Invalid bank descriptor index");
            }

            if (lb._bankDescriptorIndex > bdtVector[lb._bankLevel]) {
                bdtVector[lb._bankLevel] = lb._bankDescriptorIndex;
            }
        }

        //  Clear things out, then (maybe) set up the fixed arena
        //  Word 0 is a sentinel, word 1 is the next free offset, word 2 is the remaining space
        InventoryManager im = InventoryManager.getInstance();
        MainStorageProcessor msp = im.getMainStorageProcessor(mainStorageProcessorUPI);
        InstructionProcessor ip = im.getInstructionProcessor(instructionProcessorUPI);
        msp.clear();
        ip.clear();

        if (useFixedStorage) {
            ArraySlice fixedSlice = msp.getStorage(0);
            int fixedSize = msp.getStorage(0).getSize();
            int nextOffset = 3;
            int remaining = fixedSize - 3;
            fixedSlice.set(0, 0_777000_777000L);
            fixedSlice.set(1, nextOffset);
            fixedSlice.set(2, remaining);
        }

        //  Create all necessary bank descriptor tables
        AbsoluteAddress[] bdtAddresses = new AbsoluteAddress[8];
        for (int bx = 0; bx < 8; ++bx) {
            int highestBDI = bdtVector[bx];
            if (highestBDI >= 0) {
                int bdtSize = 8 * (highestBDI + 1);
                bdtAddresses[bx] = iplAllocate(moduleName, msp, bdtSize, useFixedStorage);
            }
        }

        //  Load the banks, creating bank descriptors along the way
        for (LoadableBank lb : loadableBanks) {
            AbsoluteAddress bdtAddr = bdtAddresses[lb._bankLevel];
            InstructionProcessor.BankDescriptor bd =
                new InstructionProcessor.BankDescriptor(msp.getStorage(bdtAddr._segment), bdtAddr._offset);

            AbsoluteAddress bankAddr = iplAllocate(moduleName, msp, lb._content.length, useFixedStorage);
            ArraySlice bankStorage = msp.getStorage(bankAddr._segment);
            bankStorage.load(lb._content, 0, lb._content.length, bankAddr._offset);

            bd.setBaseAddress(bankAddr);
            bd.setLowerLimit(lb._lowerLimit);
            bd.setUpperLimit(lb._upperLimit);
            bd.setAccessLock(lb._accessInfo);
            bd.setUpperLimitSuppressionControl(false);
            bd.setLargeBank(false);
            bd.setGeneralAccessPermissions(lb._generalPermissions);
            bd.setSpecialAccessPermissions(lb._specialPermissions);
            bd.setGeneralFault(false);
            bd.setBankType(lb._bankType);
        }

        //  Set up the initial program load (class 29) interrupt vector to point to the first word in the first bank
        ArraySlice ihVectors = msp.getStorage(bdtAddresses[0]._segment);
        VirtualAddress ihAddr = new VirtualAddress(loadableBanks[0]._bankLevel,
                                                   loadableBanks[0]._bankDescriptorIndex,
                                                   loadableBanks[0]._lowerLimit);
        ihVectors.set(29, ihAddr.getW());

        //  Establish bank registers B16 and upwards corresponding to the created bank descriptor tables
        for (int bdtLevel = 0; bdtLevel < 8; ++bdtLevel) {
            int highestBDI = bdtVector[bdtLevel];
            if (highestBDI >= 0) {
                int bdtSize = 8 * (highestBDI + 1);
                InstructionProcessor.BaseRegister br =
                    new InstructionProcessor.BaseRegister(bdtAddresses[bdtLevel],
                                                          false,
                                                          0,
                                                          bdtSize - 1,
                                                          new AccessInfo(0, 0),
                                                          new AccessPermissions(false, false, false),
                                                          new AccessPermissions(true, true, true));
                ip.setBaseRegister(16 + bdtLevel, br);
            }
        }

        //  Create and base the config bank
        if (createConfigBank) {
            //  TODO
        }

        //  Set up a small interrupt control stack (ICS)
        int icsSize = 4 * 16;
        AbsoluteAddress icsAddr = iplAllocate(moduleName, msp, icsSize, useFixedStorage);
        InstructionProcessor.BaseRegister icsBaseReg =
            new InstructionProcessor.BaseRegister(icsAddr,
                                                  false,
                                                  0,
                                                  icsSize - 1,
                                                  new AccessInfo(0, 0),
                                                  new AccessPermissions(false, false, false),
                                                  new AccessPermissions(true, true, false));
        ip.setBaseRegister(InstructionProcessor.ICS_BASE_REGISTER, icsBaseReg);
        ip.setGeneralRegister(InstructionProcessor.ICS_INDEX_REGISTER, (4 << 18) | icsSize);

        //TODO do we need to set anything else up on the IP? I don't think so...?
        upiSendDirected(instructionProcessorUPI);

        LOGGER.traceExit(em);
    }

//    /**
//     * Loads an AbsoluteModule object into the processor complex.
//     * This module has some specific requirements:
//     *      It *must* have only level 0 bank descriptors
//     *      It *must not* have any BDI's < 000040.
//     *      It *may* have a bank 000040 defined - if so, this is the pre-built level 0 BDT
//     *      It *must* have a bank dedicated to the ICS, initially based on B26
//     *      Any bank which is NOT a level 0 BDT *must* have a BDI >= 000041.
//     *      It *must* have an entry point defined.
//     *      The entry point bank must be initially based on B0 if EXTENDED MODE, or B12-B15 if BASIC MODE
//     * Caller should clear the dynamic segments out of the MSP before calling here, but it's not critical to do so.
//     * Caller should quite definitely stop the IP and clear it, before claling here.
//     * @param module AbsoluteModule to be loaded
//     * @param mainStorageProcessorUPI UPI of the MSP to be loaded
//     * @param useFixedStorage true to use fixed MSP storage; false to use dynamically-allocated segments
//     * @param instructionProcessorUPI UPI of the IP to be loaded
//     */
//    void loadAbsoluteModule(
//        final AbsoluteModule module,
//        final int mainStorageProcessorUPI,
//        final boolean useFixedStorage,
//        final int instructionProcessorUPI
//    ) throws AddressingExceptionInterrupt,
//             BinaryLoadException,
//             MachineInterrupt,
//             UPINotAssignedException,
//             UPIProcessorTypeException {
//        EntryMessage em = _logger.traceEntry("loadAbsoluteModule(module=%s mspupi=%d fixed=%s ipupi=%d",
//                                             module._name,
//                                             mainStorageProcessorUPI,
//                                             useFixedStorage,
//                                             instructionProcessorUPI);
//
//        InventoryManager im = InventoryManager.getInstance();
//        MainStorageProcessor msp = im.getMainStorageProcessor(mainStorageProcessorUPI);
//        InstructionProcessor ip = im.getInstructionProcessor(instructionProcessorUPI);
//
//        if (module._entryPointBank == null) {
//            throw new BinaryLoadException(module._name, "Module does not have an entry point defined");
//        }
//
//        LoadableBank entryPointBank = module._entryPointBank;
//        if (entryPointBank._initialBaseRegister == null) {
//            throw new BinaryLoadException(module._name, "Entry point bank is not initially based");
//        } else if (entryPointBank._isExtendedMode && (entryPointBank._initialBaseRegister != 0)) {
//            throw new BinaryLoadException(module._name, "Extended mode entry point is in a bank not initially based on B0");
//        } else if (!entryPointBank._isExtendedMode &&
//                   ((entryPointBank._initialBaseRegister < 12 || (entryPointBank._initialBaseRegister > 15)))) {
//            throw new BinaryLoadException(module._name, "Basic mode entry point is in a bank not initially based on B12-B15");
//        }
//        int entryPointAddress = module._entryPointAddress == null ? entryPointBank._startingAddress : module._entryPointAddress;
//
//        //  map of allocated banks - keyed by BDI, value is the initialized bank descriptor (in the BDT) for the loaded bank
//        Map<Integer, InstructionProcessor.BankDescriptor> bankMap = new HashMap<>();
//        ArraySlice level0Storage = null;
//
//        //  If the module doesn't have a level 0 BDT, create one.
//        //  If it *does* have one, it will be the first bank we encounter in the subsequent loop.
//        int level0BDTBDI = 000040;
//        if (!module._loadableBanks.containsKey(level0BDTBDI)) {
//            int highestBDI = module._loadableBanks.lastEntry().getKey();
//            int bdtSize = (highestBDI + 1) * 8;
//            int lowerAddressingLimit = 01000;
//
//            int segmentIndex = msp.createSegment(bdtSize);
//            level0Storage = msp.getStorage(segmentIndex);
//            AbsoluteAddress addr = new AbsoluteAddress(mainStorageProcessorUPI, segmentIndex, 0);
//
//            InstructionProcessor.BankDescriptor bd = new InstructionProcessor.BankDescriptor(level0Storage, level0BDTBDI * 8);
//            bd.setGeneralAccessPermissions(new AccessPermissions(true, true, true));
//            bd.setSpecialAccessPermissions(new AccessPermissions(true, true, true));
//            bd.setBankType(InstructionProcessor.BankType.ExtendedMode);
//            bd.setGeneralFault(false);
//            bd.setLargeBank(false);
//            bd.setUpperLimitSuppressionControl(false);
//            bd.setAccessLock(new AccessInfo(0, 0));
//            bd.setBaseAddress(addr);
//            bd.setLowerLimit(lowerAddressingLimit);
//            bd.setUpperLimit(lowerAddressingLimit + bdtSize - 1);
//
//            bankMap.put(level0BDTBDI, bd);
//        }
//
//        //  Load the banks from the module
//        for (LoadableBank loadableBank : module._loadableBanks.values()) {
//            int bdi = loadableBank._bankDescriptorIndex;
//            if (bdi < 000040) {
//                throw new BinaryLoadException(module._name,
//                                              String.format("Invalid BDI %06o", bdi));
//            }
//
//            int level = loadableBank._bankLevel;
//            if (level != 0) {
//                throw new BinaryLoadException(module._name,
//                                              String.format("Bank with BDI %06o has a non-zero bank-level", bdi));
//            }
//
//            int segmentIndex = msp.createSegment(loadableBank._content.getSize());
//            ArraySlice arraySlice = msp.getStorage(segmentIndex);
//            arraySlice.load(loadableBank._content, 0);
//            AbsoluteAddress addr = new AbsoluteAddress(mainStorageProcessorUPI, segmentIndex, 0);
//
//            if (bdi == 000040) {
//                level0Storage = arraySlice;
//            }
//
//            InstructionProcessor.BankDescriptor bd = new InstructionProcessor.BankDescriptor(level0Storage, bdi * 8);
//            bd.setGeneralAccessPermissions(loadableBank._generalPermissions);
//            bd.setSpecialAccessPermissions(loadableBank._specialPermissions);
//            bd.setBankType(loadableBank._isExtendedMode
//                           ? InstructionProcessor.BankType.ExtendedMode
//                           : InstructionProcessor.BankType.BasicMode);
//            bd.setGeneralFault(false);
//            bd.setLargeBank(false);
//            bd.setUpperLimitSuppressionControl(false);
//            bd.setAccessLock(loadableBank._accessInfo);
//            bd.setBaseAddress(addr);
//            bd.setLowerLimit(loadableBank._startingAddress >> 9);
//            bd.setUpperLimit(loadableBank._startingAddress + arraySlice.getSize() - 1);
//
//            bankMap.put(loadableBank._bankDescriptorIndex, bd);
//
//            if (loadableBank._initialBaseRegister != null) {
//                int brx = loadableBank._initialBaseRegister;
//                ip.setBaseRegister(brx, new InstructionProcessor.BaseRegister(bd));
//                if ((brx > 0) && (brx < 16)) {
//                    InstructionProcessor.ActiveBaseTableEntry abte
//                        = new InstructionProcessor.ActiveBaseTableEntry(loadableBank._bankLevel,
//                                                                        loadableBank._bankDescriptorIndex,
//                                                                        0);
//                    ip.loadActiveBaseTableEntry(brx, abte);
//                }
//            }
//        }
//
//        //  Set up certain processor registers.
//        ip.setBaseRegister(16, new InstructionProcessor.BaseRegister(bankMap.get(level0BDTBDI)));
//        InstructionProcessor.DesignatorRegister dr = ip.getDesignatorRegister();
//        dr.clear();
//        dr.setArithmeticExceptionEnabled(module._afcmSet);
//        dr.setBasicModeBaseRegisterSelection(!entryPointBank._isExtendedMode);
//        dr.setBasicModeEnabled(!entryPointBank._isExtendedMode);
//        dr.setDeferrableInterruptEnabled(true);
//        dr.setExecRegisterSetSelected(true);
//        dr.setExecutive24BitIndexingEnabled(true);
//        dr.setQuarterWordModeEnabled(module._setQuarter);
//        dr.setProcessorPrivilege(0);
//
//        InstructionProcessor.ProgramAddressRegister par = ip.getProgramAddressRegister();
//        par.setProgramCounter(entryPointAddress);
//        par.setLBDI(entryPointBank._bankDescriptorIndex);
//
//        InstructionProcessor.BaseRegister b26 = ip.getBaseRegister(26);
//        if ((b26 == null) || (b26._voidFlag)) {
//            throw new BinaryLoadException(module._name, "Missing or invalid ICS bank");
//        }
//
//        long ex1Value = (16L << 18) | (b26._upperLimitNormalized + 1);
//        ip.setGeneralRegister(GeneralRegisterSet.EX1, ex1Value);
//
//        _logger.traceExit(em);
//    }

    /**
     * Updates the credentials required for using the SPIF
     * @param credentials new credentials
     */
    void setCredentials(
        final Credentials credentials
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}()",
                                            this.getClass().getSimpleName(),
                                            "setCredentials");
        _credentials = credentials;
        LOGGER.traceExit(em);
    }

    /**
     * Stops the current HTTP listener (if any), sets the new port number, and attempts to restart it.
     * Only applies to SPs with an HTTPSystemControlInterface.
     * @param httpPort new http port, 0 to disable
     */
    boolean setHttpPort(
        final int httpPort
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(httpPort=%d)",
                                            this.getClass().getSimpleName(),
                                            "setHttpPort",
                                            httpPort);

        boolean result = false;
        _httpPort = httpPort;
        if (_systemConsoleInterface instanceof HTTPSystemProcessorInterface) {
            result = ((HTTPSystemProcessorInterface) _systemConsoleInterface).setNewHttpPort(_httpPort);
        }

        LOGGER.traceExit(em, result);
        return result;
    }

    /**
     * Stops the current HTTP listener (if any), sets the new port number, and attempts to restart it
     * Only applies to SPs with an HTTPSystemControlInterface.
     * @param httpsPort new https port, 0 to disable
     */
    boolean setHttpsPort(
        final int httpsPort
    ) {
        EntryMessage em = LOGGER.traceEntry("{}.{}(httpPort=%d)",
                                            this.getClass().getSimpleName(),
                                            "setHttpsPort",
                                            httpsPort);

        boolean result = false;
        _httpsPort = httpsPort;
        if (_systemConsoleInterface instanceof HTTPSystemProcessorInterface) {
            result = ((HTTPSystemProcessorInterface) _systemConsoleInterface).setNewHttpsPort(_httpsPort);
        }

        LOGGER.traceExit(em, result);
        return result;
    }
}
