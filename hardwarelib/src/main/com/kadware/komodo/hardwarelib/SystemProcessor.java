/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.AbsoluteAddress;
import com.kadware.komodo.baselib.AccessInfo;
import com.kadware.komodo.baselib.AccessPermissions;
import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BankDescriptor;
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

    private final Logger _logger = LogManager.getLogger(SystemProcessor.class.getSimpleName());

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
        super(ProcessorType.SystemProcessor, name, InventoryManager.FIRST_SP_UPI_INDEX);
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
        super(ProcessorType.SystemProcessor, "SP0", InventoryManager.FIRST_SP_UPI_INDEX);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Implementations / overrides of abstract base methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Clears the processor - actually, we never get cleared
     */
    @Override public void clear() {}

    /**
     * For debugging
     * @param writer destination for output
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        //TODO anything local to us
        //TODO ConfigDataBank if it exists
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
        EntryMessage em = _logger.traceEntry("worker thread");

        _systemConsoleInterface = new HTTPSystemProcessorInterface(this,
                                                                    _name + "-SPIF",
                                                                   _httpPort,
                                                                   _httpsPort);
        _systemConsoleInterface.start();

        _isReady = true;
        _logger.info(_systemConsoleInterface.getName() + " Ready");
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
                    _logger.trace(String.format("%s received a UPI ACK from %s", _name, source._name));
                    didSomething = true;
                }
                _upiPendingAcknowledgements.clear();
            }

            //  SENDs mean an IO is completed
            synchronized (_upiPendingInterrupts) {
                for (Processor source : _upiPendingInterrupts) {
                    //TODO
                    _logger.trace(String.format("%s received a UPI interrupt from %s", _name, source._name));
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
                    _logger.catching(ex);
                }
            }
        }

        _systemConsoleInterface.stop();
        _systemConsoleInterface = null;
        _logger.traceExit(em);
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
        EntryMessage em = _logger.traceEntry("consoleCancelReadReplyMessage(consoleId={} messageId={} text='{}')",
                                             consoleId,
                                             messageId,
                                             replacementText);
        _systemConsoleInterface.cancelReadReplyMessage(consoleId, messageId, replacementText);
        _logger.traceExit(em);
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
        EntryMessage em = _logger.traceEntry("consolePollInputMessage(waitMilliseconds={}})", waitMilliseconds);
        SystemProcessorInterface.ConsoleInputMessage result = _systemConsoleInterface.pollInputMessage(waitMilliseconds);
        _logger.traceExit(em, result);
        return result;
    }

    /**
     * Resets the conceptual system console
     */
    void consoleReset() {
        EntryMessage em = _logger.traceEntry("consoleReset()");
        _systemConsoleInterface.reset();
        _logger.traceExit(em);
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
        EntryMessage em = _logger.traceEntry("consoleSendReadOnlyMessage(consoleId={} message='{}' rightJust={} cached={})",
                                             consoleId,
                                             message,
                                             rightJustified,
                                             cached);
        _systemConsoleInterface.postReadOnlyMessage(consoleId, message, rightJustified, cached);
        _logger.traceExit(em);
    }

    /**
     * Convenience wrapper for the above...
     */
    void consoleSendReadOnlyMessage(
        final int consoleId,
        final String message
    ) {
        EntryMessage em = _logger.traceEntry("consoleSendReadOnlyMessage(consoleId={} message='{}')", consoleId, message);
        _systemConsoleInterface.postReadOnlyMessage(consoleId, message, false, true);
        _logger.traceExit(em);
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
        EntryMessage em = _logger.traceEntry("consoleSendReadReplyMessage(cnsId={} msgId={} msg='{}' maxReplyLength={})",
                                             consoleId,
                                             messageId,
                                             message,
                                             maxReplyLength);
        _systemConsoleInterface.postReadReplyMessage(consoleId, messageId, message, maxReplyLength);
        _logger.traceExit(em);
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
        EntryMessage em = _logger.traceEntry("consoleSendStatusMessage(#messages={})", messages.length);
        _systemConsoleInterface.postStatusMessages(messages);
        _logger.traceExit(em);
    }

    /**
     * Retrieves the master dayclock time in microseconds since epoch.
     * This time is based on the host system time, offset by a value to allow the emulated system time
     * to differ from the host system.
     */
    long dayclockGetMicros() {
        EntryMessage em = _logger.traceEntry("dayclockGetMicros()");

        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        long result = systemMicros + _dayclockOffsetMicros;

        _logger.traceExit(em, result);
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
        EntryMessage em = _logger.traceEntry("dayclockSetComparatorMicros({})", value);
        _dayclockComparatorMicros = value;
        _logger.traceExit(em);
    }

    /**
     * Stores the difference between the requested dayclock time in microseconds, and the actual host system time
     * converted to dayclock microseconds.  Subsequent dayclock reads must apply this offset.
     */
    void dayclockSetMicros(
        final long value
    ) {
        EntryMessage em = _logger.traceEntry("dayclockSetMicros({})", value);

        Instant i = Instant.now();
        long instantSeconds = i.getLong(ChronoField.INSTANT_SECONDS);
        long microOfSecond = i.getLong(ChronoField.MICRO_OF_SECOND);
        long systemMicros = (instantSeconds * 1000 * 1000) + microOfSecond;
        _dayclockOffsetMicros = value - systemMicros;

        _logger.traceExit(em);
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
        EntryMessage em = _logger.traceEntry("iplAllocate({} msp:{} words:{} fixed:{})",
                                             moduleName,
                                             mainStorageProcessor._name,
                                             words,
                                             useFixedStorage);

        AbsoluteAddress result;
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

            result = new AbsoluteAddress(mainStorageProcessor._upiIndex, 0, nextOffset);
            nextOffset += words;
            remaining -= words;
            fixedSlice.set(1, nextOffset);
            fixedSlice.set(2, remaining);
        } else {
            int segIndex = mainStorageProcessor.createSegment(words);
            result = new AbsoluteAddress(mainStorageProcessor._upiIndex, segIndex, 0);
        }

        _logger.traceExit(em, result);
        return result;
    }

    /**
     * Loads a single or multi-banked binary produced by the linker, into cleared memory.
     * Produces a level 0 bank descriptor table and any other necessary BDTs.
     * Sets up minimal interrupt handling, sufficient for IPL to succeed
     * Sets up bank registers in the given IP to represent the bank descriptor tables
     * Sets up the designator register for ring 0, level 0, exec mode, etc
     * Sets the PAR,PC per the starting address (see above)
     * Optionally creates a configuration bank, and bases it on B2 for extended mode, or B13 for basic mode
     *      this bank is NOT in any bank descriptor table, so if you unbase it, you cannot get it back
     * Finally, sends an IPL to the indicated instruction processor to get things rolling.
     * Does NOT clear the processors - calling code must set desired state before invoking here.
     * @param moduleName name of the module being loaded
     * @param loadableBanks Represents the code to be loaded
     * @param startAddress Virtual address at which execution begins
     * @param mainStorageProcessorUPI UPI of the MSP to be loaded
     * @param instructionProcessorUPI UPI of the IP to be loaded
     * @param useFixedStorage true to use fixed MSP storage; false to use dynamically-allocated segments
     */
    public void iplBinary(
        final String moduleName,
        final LoadableBank[] loadableBanks,
        final VirtualAddress startAddress,
        final int mainStorageProcessorUPI,
        final int instructionProcessorUPI,
        final boolean useFixedStorage,
        final boolean createConfigBank
    ) throws BinaryLoadException,
             MachineInterrupt,
             UPINotAssignedException,
             UPIProcessorTypeException {
        EntryMessage em = _logger.traceEntry("iplLoadBinary({} start={} msp={} ip={}, fixed={} config={}",
                                            moduleName,
                                            startAddress,
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

        //  Load the banks, creating bank descriptors along the way.
        //  Banks with no content are created such that they will be treated as void banks
        //  if and when they are based.
        for (LoadableBank lb : loadableBanks) {
            AbsoluteAddress bdtAddr = bdtAddresses[lb._bankLevel];
            ArraySlice bdtSlice = msp.getStorage(bdtAddr._segment);
            int bdtOffset = bdtAddr._offset;
            int bdOffset = bdtOffset + (8 * lb._bankDescriptorIndex);
            BankDescriptor bd = new BankDescriptor(bdtSlice, bdOffset);

            if (lb._content.length > 0) {

                AbsoluteAddress bankAddr = iplAllocate(moduleName, msp, lb._content.length, useFixedStorage);
                ArraySlice bankStorage = msp.getStorage(bankAddr._segment);
                bankStorage.load(lb._content, 0, lb._content.length, bankAddr._offset);

                bd.setBaseAddress(bankAddr);
                bd.setLowerLimitNormalized(lb._lowerLimit);
                bd.setUpperLimitNormalized(lb._upperLimit);
            } else {
                bd.setBaseAddress(new AbsoluteAddress(0,0,0));
                bd.setLowerLimitNormalized(01000);
                bd.setUpperLimitNormalized(0777);
            }

            bd.setAccessLock(lb._accessInfo);
            bd.setUpperLimitSuppressionControl(false);
            bd.setLargeBank(false);
            bd.setGeneralAccessPermissions(lb._generalPermissions);
            bd.setSpecialAccessPermissions(lb._specialPermissions);
            bd.setGeneralFault(false);
            bd.setBankType(lb._bankType);
        }

        //  Set up the initial program load (class 29) interrupt vector to point to the starting address.
        ArraySlice ihVectors = msp.getStorage(bdtAddresses[0]._segment);
        ihVectors.set(29, startAddress.getW());

        //  Establish bank registers B16 and upwards corresponding to the created bank descriptor tables
        for (int bdtLevel = 0; bdtLevel < 8; ++bdtLevel) {
            int highestBDI = bdtVector[bdtLevel];
            if (highestBDI >= 0) {
                int bdtSize = 8 * (highestBDI + 1);
                BaseRegister br =
                    new BaseRegister(bdtAddresses[bdtLevel],
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
        //  TODO
        if (createConfigBank) {
        }

        //  Set up a small interrupt control stack (ICS).
        int icsFrameSize = 16;
        int icsStackEntries = 4;
        int icsSize = icsFrameSize * icsStackEntries;
        AbsoluteAddress icsAddr = iplAllocate(moduleName, msp, icsSize, useFixedStorage);
        BaseRegister icsBaseReg =
            new BaseRegister(icsAddr,
                             false,
                             0,
                                                  icsSize - 1,
                             new AccessInfo(0, 0),
                             new AccessPermissions(false, false, false),
                             new AccessPermissions(true, true, false));
        ip.setBaseRegister(InstructionProcessor.ICS_BASE_REGISTER, icsBaseReg);
        ip.setGeneralRegister(InstructionProcessor.ICS_INDEX_REGISTER, (icsFrameSize << 18) | icsSize);

        upiSendDirected(instructionProcessorUPI);
        _logger.traceExit(em);
    }

    void populateConfigDataBank() {
        ConfigDataBank cdb = new ConfigDataBank();

    }

    /**
     * Updates the credentials required for using the SPIF
     * @param credentials new credentials
     */
    void setCredentials(
        final Credentials credentials
    ) {
        EntryMessage em = _logger.traceEntry("setCredentials(<sensitive data>)");
        _credentials = credentials;
        _logger.traceExit(em);
    }

    /**
     * Stops the current HTTP listener (if any), sets the new port number, and attempts to restart it.
     * Only applies to SPs with an HTTPSystemControlInterface.
     * @param httpPort new http port, 0 to disable
     */
    boolean setHttpPort(
        final int httpPort
    ) {
        EntryMessage em = _logger.traceEntry("setHttpPort({})", httpPort);

        boolean result = false;
        _httpPort = httpPort;
        if (_systemConsoleInterface instanceof HTTPSystemProcessorInterface) {
            result = ((HTTPSystemProcessorInterface) _systemConsoleInterface).setNewHttpPort(_httpPort);
        }

        _logger.traceExit(em, result);
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
        EntryMessage em = _logger.traceEntry("setHttpsPort({})", httpsPort);

        boolean result = false;
        _httpsPort = httpsPort;
        if (_systemConsoleInterface instanceof HTTPSystemProcessorInterface) {
            result = ((HTTPSystemProcessorInterface) _systemConsoleInterface).setNewHttpsPort(_httpsPort);
        }

        _logger.traceExit(em, result);
        return result;
    }
}
