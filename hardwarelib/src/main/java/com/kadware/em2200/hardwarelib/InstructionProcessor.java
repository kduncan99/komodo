/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import com.kadware.em2200.baselib.*;
import com.kadware.em2200.hardwarelib.exceptions.*;
import com.kadware.em2200.hardwarelib.functions.*;
import com.kadware.em2200.hardwarelib.misc.*;
import com.kadware.em2200.hardwarelib.interrupts.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class which models an Instruction Procesor node
 */
public class InstructionProcessor extends Processor implements Worker {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum RunMode {
        Normal,
        SingleInstruction,
        SingleCycle,
    };

    public enum StopReason {
        Initial,
        Cleared,
        Debug,
        Development,
        Breakpoint,
        HaltJumpExecuted,
        ICSBaseRegisterInvalid,
        ICSOverflow,
        InitiateAutoRecovery,
        L0BaseRegisterInvalid,
        PanelHalt,

        // Interrupt Handler initiated stops...
        InterruptHandlerHardwareFailure,
        InterruptHandlerOffsetOutOfRange,
        InterruptHandlerInvalidBankType,
        InterruptHandlerInvalidLevelBDI,
    };

    public enum BreakpointComparison {
        Fetch,
        Read,
        Write,
    };


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Base register for L=0 bank descriptor table
     */
    public static final int L0_BDT_BASE_REGISTER            = 16;

    /**
     * Base register for interrupt control stack
     */
    public static final int ICS_BASE_REGISTER               = 26;

    /**
     * Stack pointer register for interrupt control stack
     */
    public static final int ICS_INDEX_REGISTER              = GeneralRegisterSet.EX1;

    /**
     * Raise interrupt when this many new entries exist
     */
    private static final int JUMP_HISTORY_TABLE_THRESHOLD   = 120;

    /**
     * Size of the jump history table
     */
    private static final int JUMP_HISTORY_TABLE_SIZE        = 128;

    private static final Logger LOGGER = LogManager.getLogger(InstructionProcessor.class);

    /**
     * Order of base register selection for Basic Mode address resolution
     * when the Basic Mode Base Register Selection Designator Register bit is false
     */
    private static final int BASE_REGISTER_CANDIDATES_FALSE[] = {12, 14, 13, 15};

    /**
     * Order of base register selection for Basic Mode address resolution
     * when the Basic Mode Base Register Selection Designator Register bit is true
     */
    private static final int BASE_REGISTER_CANDIDATES_TRUE[] = {13, 15, 12, 14};

    /**
     * ActiveBaseTable entries - B1 is index 0, B15 is index 14.  There is no entry for B0.
     */
    //TODO private ActiveBaseTableEntry _activeBaseTableEntries[] = new ActiveBaseTableEntry[15];

    private static final Map<InstructionProcessor, HashSet<AbsoluteAddress>> _storageLocks = new HashMap<>();

    private final BaseRegister              _baseRegisters[] = new BaseRegister[32];
    private final AbsoluteAddress           _breakpointAddress = new AbsoluteAddress((short)0, -1);
    private final BreakpointRegister        _breakpointRegister = new BreakpointRegister();
    private boolean                         _broadcastInterruptEligibility = false;
    private final InstructionWord           _currentInstruction = new InstructionWord();
    private FunctionHandler                 _currentInstructionHandler = null;
    private RunMode                         _currentRunMode = RunMode.Normal;
    private final DesignatorRegister        _designatorRegister = new DesignatorRegister();
    private boolean                         _developmentMode = true;    //  TODO default this to false and provide a means of changing it
    private final GeneralRegisterSet        _generalRegisterSet = new GeneralRegisterSet();
    private final IndicatorKeyRegister      _indicatorKeyRegister = new IndicatorKeyRegister();
    private final InventoryManager          _inventoryManager = InventoryManager.getInstance();
    private boolean                         _jumpHistoryFullInterruptEnabled = false;
    private final Word36[]                  _jumpHistoryTable = new Word36[JUMP_HISTORY_TABLE_SIZE];
    private int                             _jumpHistoryTableNext = 0;
    private boolean                         _jumpHistoryThresholdReached = false;
    private MachineInterrupt                _lastInterrupt = null;    //  must always be != _pendingInterrupt
    private long                            _latestStopDetail = 0;
    private StopReason                      _latestStopReason = StopReason.Initial;
    private boolean                         _midInstructionInterruptPoint = false;
    private MachineInterrupt                _pendingInterrupt = null;
    private final ProgramAddressRegister    _preservedProgramAddressRegister = new ProgramAddressRegister();
    private boolean                         _preservedProgramAddressRegisterValid = false;  //  do we need this if the above can be null?
    private boolean                         _preventProgramCounterIncrement = false;
    private final ProgramAddressRegister    _programAddressRegister = new ProgramAddressRegister();
    private final Word36                    _quantumTimer = new Word36();
    private boolean                         _runningFlag = false;


    /**
     * Set this to cause the worker thread to shut down
     */
    private boolean _workerTerminate = false;

    /**
     * reference to worker thread
     */
    private final Thread _workerThread;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param name
     * @param upi unique identifier for this processor
     */
    public InstructionProcessor(
        final String name,
        final short upi
    ) {
        super(Processor.ProcessorType.InstructionProcessor, name, upi);

        _storageLocks.put(this, new HashSet<AbsoluteAddress>());

        for (int bx = 0; bx < _baseRegisters.length; ++bx) {
            _baseRegisters[bx] = new BaseRegister();
        }

        _workerThread = new Thread(this);
        _workerTerminate = false;

        for (int jx = 0; jx < JUMP_HISTORY_TABLE_SIZE; ++jx) {
            _jumpHistoryTable[jx] = new Word36();
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @param index
     * <p>
     * @return
     */
    public BaseRegister getBaseRegister(
        final int index
    ) {
        return _baseRegisters[index];
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getBroadcastInterruptEligibility(
    ) {
        return _broadcastInterruptEligibility;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public InstructionWord getCurrentInstruction(
    ) {
        return _currentInstruction;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public RunMode getCurrentRunMode(
    ) {
        return _currentRunMode;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public DesignatorRegister getDesignatorRegister(
    ) {
        return _designatorRegister;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getDevelopmentMode(
    ) {
        return _developmentMode;
    }

    /**
     * Getter
     * <p>
     * @param index GRS index
     * <p>
     * @return
     * <p>
     * @throws MachineInterrupt
     */
    public GeneralRegister getGeneralRegister(
        final int index
    ) throws MachineInterrupt {
        if (!GeneralRegisterSet.isAccessAllowed(index, _designatorRegister.getProcessorPrivilege(), false)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }

        return _generalRegisterSet.getRegister(index);
    }

    /**
     * Getter
     * @return reason for the latest stop
     */
    public StopReason getLatestStopReason(
    ) {
        return _latestStopReason;
    }

    /**
     * Getter
     * @return detail for the latest stop
     */
    public long getLatestStopDetail(
    ) {
        return _latestStopDetail;
    }

    /**
     * Getter
     * @return value
     */
    public ProgramAddressRegister getProgramAddressRegister(
    ) {
        return _programAddressRegister;
    }

    /**
     * Getter
     * @return value
     */
    public boolean getRunningFlag(
    ) {
        return _runningFlag;
    }

    /**
     * Setter
     * <p>
     * @param index
     * @param baseRegister
     */
    public void setBaseRegister(
        final int index,
        final BaseRegister baseRegister
    ) {
        _baseRegisters[index] = baseRegister;
    }

    /**
     * Setter
     * <p>
     * @param flag
     */
    public void setBroadcastInterruptEligibility(
        final boolean flag
    ) {
        _broadcastInterruptEligibility = flag;
    }

    /**
     * Setter
     * <p>
     * @param index
     * @param value
     * <p>
     * @throws MachineInterrupt
     */
    public void setGeneralRegister(
        final int index,
        final long value
    ) throws MachineInterrupt {
        if (!GeneralRegisterSet.isAccessAllowed(index, _designatorRegister.getProcessorPrivilege(), true)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
        }

        _generalRegisterSet.setRegister(index, value);
    }

    /**
     * Setter
     * <p>
     * @param flag
     */
    public void setJumpHistoryFullInterruptEnabled(
        final boolean flag
    ) {
        _jumpHistoryFullInterruptEnabled = flag;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setProgramAddressRegister(
        final long value
    ) {
        _programAddressRegister.setW(value);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * //TODO:Move this comment somewhere more appropriate
     * When an interrupt is raised and the IP recognizes such, it saves interrupt information and other machine state
     * information on the ICS (Interrupt Control Stack) and the Jump History table.  The Program Address Register is
     * updated from the vector for the particular interrupt, and a new hard-held ASP (Activity State Packet) is built.
     * Then instruction processing proceeds per normal (albeit in interrupt handling state).
     * See the hardware doc for instructions as to what machine state information needs to be preserved.
     *
     * Interrupts are recognized and processed only at specific points during instruction execution.
     * If an instruction is interrupted mid-execution, the state of that instruction must be preserved on the ICS
     * so that it can be resumed when interrupt processing is complete, except during hardware check interrupts
     * (in which case, the sequence of instruction(s) leading to the fault will not be resumed/retried).
     *
     * Instructions which can be interrupted mid-execution include
     *      BIML
     *      BICL
     *      BIMT
     *      BT
     *      EXR
     *      BAO
     *      All search instructions
     * Additionally, the EX instruction may be interrupted between each lookup of the next indirectly-referenced
     * instruction (in the case where the EX instruction refers to another EX instruction, then to another, etc).
     * Also, basic-mode indirect addressing, which also may have lengthy or infinite indirection must be
     * interruptable during the U-field resolution.
     *
     *  Nonfault interrupts are always taken at the next interrupt point (unless classified as a pended
     * interrupt; see Table 5–1), which may be either a between instructions or mid-execution interrupt
     * point. Note: the processor is not required to take asynchronous, nonfault interrupts at the next
     * interrupt point as long as the interrupt is not "locked out" longer than one millisecond. When taken
     * at a between instruction interrupt point, machine state reflects instruction completion and
     * ICS.INF = 0. When taken at a mid-execution interrupt point, hardware must back up PAR.PC to
     * point to the interrupted instruction, or the address of the EX or EXR instruction which led to the
     * interrupted instruction, and the remainder of the pertinent machine state (see below) must reflect
     * progress to that point. In this case, ICS.INF := 1.
     *
     * Fault interrupts detected during an instruction with no mid-execution interrupt point cause hardware
     * to back up pertinent machine state (as described below) to reflect the environment in effect at the
     * start of the instruction. Fault interrupts detected during an instruction with mid-execution interrupt
     * points cause hardware to back up pertinent machine state to reflect the environment at the last
     * interrupt point (which may be either a between instruction or a mid-execution interrupt point).
     * ICS.INF := 1 for all fault interrupts except those caused by the fetching of an instruction.
     *
     * B26 describes the base and limits of the bank which comprises the Interrupt Control Stack.
     * EX1 contains the ICS frame size in X(i) and the fram pointer in X(m).  Frame size must be a multiple of 16.
     * ICS frame:
     *  +0      Program Address Register
     *  +1      Designator Register
     *  +2,S1       Short Status Field
     *  +2,S2-S5    Indicator Key Register
     *  +3      Quantum TImer
     *  +4      If INF=1 in the Indicator Key Register (see 2.2.5)
     *  +5      Interrupt Status Word 0
     *  +6      Interrupt Status Word 1
     *  +7 - ?  Reserved for software
     */

    /**
     * Calculates the raw relative address (the U) for the current instruction.
     * Does NOT increment any x registers, even if their content contributes to the result.
     * <p>
     * @param offset For multiple transfer instructions which need to calculate U for each transfer,
     *                  this value increments from zero upward by one.
     * @return
     */
    private int calculateRelativeAddressForGRSOrStorage(
        final int offset
    ) {
        IndexRegister xReg = null;
        int xx = (int)_currentInstruction.getX();
        if (xx != 0) {
            xReg = getExecOrUserXRegister(xx);
        }

        long addend1;
        long addend2 = 0;
        if (_designatorRegister.getBasicModeEnabled()) {
            addend1 = _currentInstruction.getU();
            if (xReg != null) {
                addend2 = xReg.getSignedXM();
            }
        } else {
            addend1 = _currentInstruction.getD();
            if (xReg != null) {
                if (_designatorRegister.getExecutive24BitIndexingEnabled()
                        && (_designatorRegister.getProcessorPrivilege() < 2)) {
                    //  Exec 24-bit indexing is requested
                    addend2 = xReg.getSignedXM24();
                } else {
                    addend2 = xReg.getSignedXM();
                }
            }
        }

        long result = OnesComplement.add36Simple(addend1, addend2);
        if (offset != 0) {
            result = OnesComplement.add36Simple(result, offset);
        }

        return (int)result;
    }

    /**
     * Checks the given absolute address and comparison type against the breakpoint register to see whether
     * we should take a breakpoint.  Updates IKR appropriately.
     * <p>
     * @param comparison
     * @param absoluteAddress
     */
    private void checkBreakpoint(
        final BreakpointComparison comparison,
        final AbsoluteAddress absoluteAddress
    ) {
        if (_breakpointAddress.equals(absoluteAddress)
                && (((comparison == BreakpointComparison.Fetch) && _breakpointRegister.getFetchFlag())
                    || ((comparison == BreakpointComparison.Read) && _breakpointRegister.getReadFlag())
                    || ((comparison == BreakpointComparison.Write) && _breakpointRegister.getWriteFlag()))) {
            //TODO Per doc, 2.4.1.2 Breakpoint_Register - we need to halt if Halt Enable is set
            //      which means Stop Right Now... how do we do that for all callers of this code?
            _indicatorKeyRegister.setBreakpointRegisterMatchCondition(true);
        }
    }

    /**
     * If an interrupt is pending, handle it.
     * If not, check certain conditions to see if one of several certain interrupt classes needs to be raised.
     * <p>
     * @return true if we did something useful, else false
     * <p>
     * @throws MachineInterrupt if we need to cause an interrupt to be raised
     */
    private boolean checkPendingInterrupts(
    ) throws MachineInterrupt {
        //TODO
        // Is there an interrupt pending?  If so, handle it (but wait - is DB13 == 0?  Is the pending interrupt deferrable?
        //      also, do we need to enqueue interrupts in case multiple interrupts occur?  I think maybe so...?
        if (_pendingInterrupt != null) {
            handleInterrupt();
            return true;
        }

        // Are there any pending conditions which need to be turned into interrupts?
        if (_indicatorKeyRegister.getBreakpointRegisterMatchCondition() && !_midInstructionInterruptPoint) {
            if (_breakpointRegister.getHaltFlag()) {
                stop(StopReason.Breakpoint, 0);
                return true;
            } else {
                throw new BreakpointInterrupt();
            }
        }

        if (_quantumTimer.isNegative() && _designatorRegister.getQuantumTimerEnabled()) {
            throw new QuantumTimerInterrupt();
        }

        if (_indicatorKeyRegister.getSoftwareBreak() && !_midInstructionInterruptPoint) {
            throw new SoftwareBreakInterrupt();
        }

        if (_jumpHistoryThresholdReached && _jumpHistoryFullInterruptEnabled && !_midInstructionInterruptPoint) {
            throw new JumpHistoryFullInterrupt();
        }

        return false;
    }

    /**
     * Creates a new entry in the jump history table.
     * If we cross the interrupt threshold, set the threshold-reached flag.
     * <p>
     * @param value absolute address to be placed into the jump history table
     */
    private void createJumpHistoryTableEntry(
        final long value
    ) {
        _jumpHistoryTable[_jumpHistoryTableNext].setW(value);

        if (_jumpHistoryTableNext > JUMP_HISTORY_TABLE_THRESHOLD ) {
            _jumpHistoryThresholdReached = true;
        }

        if (_jumpHistoryTableNext == JUMP_HISTORY_TABLE_SIZE ) {
            _jumpHistoryTableNext = 0;
        }
    }

    /**
     * Starts or continues the process of executing the instruction in _currentInstruction.
     * Don't call this if IKR.INF is not set.
     * <p>
     * @throws MachineInterrupt
     * @throws UnresolvedAddressException
     */
    protected void executeInstruction(
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Call the function handler, then keep INF (instruction in F0) true if we have not completed
        //  the instruction (MidInstIntPt == true), or false if we are not (MidInstIntPt == false).
        //  It is up to the function handler to:
        //      * set or clear m_MidInstructionInterruptPoint as appropriate
        //      * properly store instruction mid-point state if it returns mid-instruction
        //      * detect and restore instruction mid-point state if it is subsequently called
        //          after returning in mid-point state.
        FunctionHandler handler = FunctionTable.lookup(_currentInstruction, _designatorRegister.getBasicModeEnabled());
        if (handler == null) {
            _midInstructionInterruptPoint = false;
            _indicatorKeyRegister.setInstructionInF0(false);
            throw new InvalidInstructionInterrupt(InvalidInstructionInterrupt.Reason.UndefinedFunctionCode);
        }

        handler.handle(this, _currentInstruction);
        _indicatorKeyRegister.setInstructionInF0(_midInstructionInterruptPoint);
        if (!_midInstructionInterruptPoint) {
            //  instruction is done - clear storage locks
            synchronized(_storageLocks) {
                _storageLocks.get(this).clear();
            }
        }
    }

    /**
     * Fixed object for fetchInstruction() - helps mitigate proliferation of nasty little objects
     */
    private final AbsoluteAddress _fetchInstructionAbsoluteAddress = new AbsoluteAddress();

    /**
     * Diverts code to either the basic mode or extended mode fetch handler
     * <p>
     * @throws MachineInterrupt
     */
    private void fetchInstruction(
    ) throws MachineInterrupt {
        _midInstructionInterruptPoint = false;
        if (_designatorRegister.getBasicModeEnabled()) {
            fetchInstructionBasicMode();
        } else {
            fetchInstructionExtendedMode();
        }
    }

    /**
     * Fetches the next instruction based on the current program address register,
     * and places it in the current instruction register -- for basic mode.
     * <p>
     * @throws MachineInterrupt
     */
    private void fetchInstructionBasicMode(
    ) throws MachineInterrupt {
        //  Make sure program counter is within one of the BRs 12-15.
        int baseRegisterIndex = findBasicModeBank(_programAddressRegister.getProgramCounter(), true);
        if (baseRegisterIndex == 0) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }

        BaseRegister bReg = _baseRegisters[baseRegisterIndex];

        //  Make sure it is not a large size bank.
        if (bReg._largeSizeFlag) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }

        //  Make sure we have execute permission here.
        if (!isExecuteAllowed(bReg)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
        }

        //  Make sure that we have read access to this bank (whichever bank it is).
        //  See PRM 4.4.1
        if (!isReadAllowed(bReg)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
        }

        //  No need to check limits - it is implicitly within limits of the bank described by the baseRegisterIndex
        //  due to the operation of findBasicModeBank() above.

        //  Get absolute address, compare it against the breakpoint register, and read the word from storage.
        _fetchInstructionAbsoluteAddress.set(bReg._baseAddress._upi,
                                             bReg._baseAddress._offset);
        _fetchInstructionAbsoluteAddress.addOffset(_programAddressRegister.getProgramCounter() - bReg._lowerLimitNormalized);
        checkBreakpoint(BreakpointComparison.Fetch, _fetchInstructionAbsoluteAddress);

        try {
            long value = _inventoryManager.getStorageValue(_fetchInstructionAbsoluteAddress);
            _currentInstruction.setW(value);
            _indicatorKeyRegister.setInstructionInF0(true);
            _midInstructionInterruptPoint = false;
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }
    }

    /**
     * Fetches the next instruction based on the current program address register,
     * and placces it in the current instruction register -- for extended mode.
     * <p>
     * @throws MachineInterrupt
     */
    private void fetchInstructionExtendedMode(
    ) throws MachineInterrupt {
        //  Is the bank based on BR0 void, or a large bank?
        if (_baseRegisters[0]._voidFlag || _baseRegisters[0]._largeSizeFlag) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }

        //  We don't need to check for execute permission - that check is performed at the time the bank is based on BR0.
        //  Is PAR.PC in the addressing range of the bank based on BR0?
        if ( (_programAddressRegister.getProgramCounter() > _baseRegisters[0].getUpperLimit())
            || (_programAddressRegister.getProgramCounter() < _baseRegisters[0].getLowerLimit()) ) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }

        //  Get absolute address, compare it against the breakpoint register, and read the word from storage.
        //  Set the word (which is an instruction) in _currentInstruction, clear MidInstIntPt, and set IKR.INF.
        _fetchInstructionAbsoluteAddress.set(_baseRegisters[0]._baseAddress._upi,
                                             _baseRegisters[0]._baseAddress._offset);
        int offset = _programAddressRegister.getProgramCounter() - _baseRegisters[0]._lowerLimitNormalized;
        _fetchInstructionAbsoluteAddress.addOffset(offset);
        checkBreakpoint(BreakpointComparison.Fetch, _fetchInstructionAbsoluteAddress);

        try {
            long value = _inventoryManager.getStorageValue(_fetchInstructionAbsoluteAddress);
            _currentInstruction.setW(value);
            _indicatorKeyRegister.setInstructionInF0(true);
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }
    }

    /**
     * Given a relative address, we determine which (if any) of the basic mode banks based on BDR12-15
     * are to be selected for that address.
     * We do NOT evaluate whether the bank has any particular permissions, or whether we have any access thereto.
     * <p>
     * @param relativeAddress relative address for which we search for a containing bank
     * @param updateDB31 set true to update DB31 if we cross primary/secondary bank pairs
     * <p>
     * @return the bank register index for the bank which contains the given relative address if found,
     *          else zero if the address is not within any based bank limits.
     */
    private int findBasicModeBank(
        final int relativeAddress,
        final boolean updateDB31
    ) {
        boolean db31Flag = _designatorRegister.getBasicModeBaseRegisterSelection();
        int[] table = db31Flag ? BASE_REGISTER_CANDIDATES_TRUE : BASE_REGISTER_CANDIDATES_FALSE;

        for (int tx = 0; tx < 4; ++tx) {
            //  See IP PRM 4.4.5 - select the base register from the selection table.
            //  If the bank is void, skip it.
            //  If the program counter is outside of the bank limits, skip it.
            //  Otherwise, we found the BDR we want to use.
            BaseRegister bReg = _baseRegisters[table[tx]];
            if (isWithinLimits(bReg, relativeAddress)) {
                if (updateDB31 && (tx >= 2)) {
                    //  address is found in a secondary bank, so we need to flip DB31
                    _designatorRegister.setBasicModeBaseRegisterSelection(!db31Flag);
                }

                return table[tx];
            }
        }

        return 0;
    }

    /**
     * Locates the index of the base register which represents the bank which contains the given relative address.
     * Does appropriate limits checking.  Delegates to the appropriate basic or extended mode implementation.
     * <p>
     * @param relativeAddress relative address to be considered
     * @param writeAccess indicates the caller intends a write operation - if false, a read is intended
     * <p>
     * @return
     * <p>
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    private int findBaseRegisterIndex(
        final int relativeAddress,
        final boolean writeAccess
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        if (_designatorRegister.getBasicModeEnabled()) {
            return findBaseRegisterIndexBasicMode(relativeAddress, writeAccess);
        } else {
            return findBaseRegisterIndexExtendedMode(relativeAddress, writeAccess);
        }
    }

    /**
     * Private fixed AbsoluteAddress object for findBaseRegister* methods.
     * Likely, we'll only use it during BasicMode indirect address resolution.
     */
    private final AbsoluteAddress _fbrAbsoluteAddress = new AbsoluteAddress();

    /**
     * Locates the index of the base register which represents the bank which contains the given relative address.
     * Does appropriate limits checking.
     * <p>
     * @param relativeAddress relative address to be considered
     * @param writeAccess indicates the caller intends a write operation - if false, a read is intended
     * <p>
     * @return
     * <p>
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    private int findBaseRegisterIndexBasicMode(
        final int relativeAddress,
        final boolean writeAccess
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Find the bank containing the current offset.
        //  We don't need to check for storage limits, since this is done for us by findBasicModeBank().
        int brIndex = findBasicModeBank(relativeAddress, false);
        if (brIndex == 0) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        BaseRegister bReg = _baseRegisters[brIndex];

        //  Are we doing indirect addressing?
        if (_currentInstruction.getI() != 0) {
            //  Increment the X register (if any) indicated by F0 (if H bit is set, of course)
            incrementIndexRegisterInF0();

            //  Ensure we can read from the selected bank
            if (!isReadAllowed(bReg)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
            }

            //  Get xhiu fields from the referenced word, and place them into _currentInstruction,
            //  then throw UnresolvedAddressException so the caller knows we're not done here.
            try {
                _fbrAbsoluteAddress.set(bReg._baseAddress._upi, bReg._baseAddress._offset);
                _fbrAbsoluteAddress.addOffset(relativeAddress - bReg._lowerLimitNormalized);
                long replacementValue = _inventoryManager.getStorageValue(_fbrAbsoluteAddress);
                _currentInstruction.setXHIU(replacementValue);
            } catch (AddressLimitsException
                     | UPINotAssignedException
                     | UPIProcessorTypeException ex) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
            }

            throw new UnresolvedAddressException();
        }

        //  We're at our final destination.  Check accessibility
        if (writeAccess) {
            if (!isWriteAllowed(bReg)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
            }
        } else {
            if (!isReadAllowed(bReg)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
            }
        }

        return brIndex;
    }

    /**
     * Locates the index of the base register which represents the bank which contains the given relative address.
     * Does appropriate limits checking.
     * <p>
     * @param relativeAddress relative address to be considered
     * @param writeAccess indicates the caller intends a write operation - if false, a read is intended
     * <p>
     * @return generated relative address.
     * <p>
     * @return
     * <p>
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     */
    private int findBaseRegisterIndexExtendedMode(
        final int relativeAddress,
        final boolean writeAccess
    ) throws MachineInterrupt {
        int brIndex = getEffectiveBaseRegisterIndex();
        BaseRegister bReg = _baseRegisters[brIndex];
        if (bReg._voidFlag) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        //  check limits of displacement and accessibility
        if ((!isWithinLimits(bReg, relativeAddress))
                || (!writeAccess && !isReadAllowed(bReg))
                || (writeAccess && !isWriteAllowed(bReg))) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        return brIndex;
    }

    /**
     * Determines the base register to be used for an extended mode instruction,
     * using the designator bit to indicate whether to use exec or user banks,
     * and whether we are using the I bit to extend the B field.
     * (Exec base registers are B16-B31).
     * <p>
     * @return
     */
    private int getEffectiveBaseRegisterIndex(
    ) {
        //  If PP < 2, we use the i-bit and the b-field to select the base registers from B0 to B31.
        //  For PP >= 2, we only use the b-field, to select base registers from B0 to B15 (See IP PRM 4.3.7).
        if (_designatorRegister.getProcessorPrivilege() < 2) {
            return (int)_currentInstruction.getIB();
        } else {
            return (int)_currentInstruction.getB();
        }
    }

    /**
     * Retrieves the AccessPermissions object applicable for the bank described by the given baseRegister,
     * within the context of our current key/ring.
     * <p>
     * @param baseRegister
     * <p>
     * @return
     */
    private AccessPermissions getEffectivePermissions(
        final BaseRegister baseRegister
    ) {
        AccessInfo tempInfo = new AccessInfo(_indicatorKeyRegister.getAccessKey());

        // If we are at a more-privileged ring than the base register's ring, use the base register's special access permissions.
        if (tempInfo._ring < baseRegister._accessLock._ring) {
            return baseRegister._specialAccessPermissions;
        }

        // If we are in the same domain as the base register, again, use the special access permissions.
        if (tempInfo._domain == baseRegister._accessLock._domain) {
            return baseRegister._specialAccessPermissions;
        }

        // Otherwise, use the general access permissions.
        return baseRegister._generalAccessPermissions;
    }

    /**
     * Retrieves the GRS index of the exec or user register indicated by the register index...
     * i.e., registerIndex == 0 returns the GRS index for either R0 or ER0, depending on the designator register.
     * <p>
     * @param registerIndex
     * <p>
     * @return
     */
    private int getExecOrUserRRegisterIndex(
        final int registerIndex
    ) {
        return registerIndex + (_designatorRegister.getExecRegisterSetSelected() ? GeneralRegisterSet.ER0 : GeneralRegisterSet.R0);
    }

    /**
     * Retrieves the GRS index of the exec or user register indicated by the register index...
     * i.e., registerIndex == 0 returns the GRS index for either X0 or EX0, depending on the designator register.
     * <p>
     * @param registerIndex
     * <p>
     * @return
     */
    private int getExecOrUserXRegisterIndex(
        final int registerIndex
    ) {
        return registerIndex + (_designatorRegister.getExecRegisterSetSelected() ? GeneralRegisterSet.EX0 : GeneralRegisterSet.X0);
    }

    /**
     * Handles the current pending interrupt.  Do not call if no interrupt is pending.
     * <p>
     * @throws MachineInterrupt
     */
    private void handleInterrupt(
    ) throws MachineInterrupt {
        // Get pending interrupt, save it to lastInterrupt, and clear pending.
        //TODO are interrupts prevented?  If so, do not handle a deferable interrupt
        MachineInterrupt interrupt = _pendingInterrupt;
        _pendingInterrupt = null;
        _lastInterrupt = interrupt;

        // Update interrupt-specific portions of the IKR
        _indicatorKeyRegister.setShortStatusField(interrupt.getShortStatusField());
        _indicatorKeyRegister.setInterruptClassField(interrupt.getInterruptClass().getCode());

        // Make sure the interrupt control stack base register is valid
        if (_baseRegisters[ICS_BASE_REGISTER]._voidFlag) {
            stop(StopReason.ICSBaseRegisterInvalid, 0);
            return;
        }

        // Acquire a stack frame, and verify limits
        IndexRegister icsXReg = (IndexRegister)_generalRegisterSet.getRegister(ICS_INDEX_REGISTER);
        icsXReg.decrementModifier18();
        long stackOffset = icsXReg.getH2();
        long stackFrameSize = icsXReg.getXI();
        long stackFrameLimit = stackOffset + stackFrameSize;
        if ((stackFrameLimit - 1 > _baseRegisters[ICS_BASE_REGISTER]._upperLimitNormalized)
            || (stackOffset < _baseRegisters[ICS_BASE_REGISTER]._lowerLimitNormalized)) {
            stop(StopReason.ICSOverflow, 0);
            return;
        }

        // Populate the stack frame in storage.
        Word36Array icsStorage = _baseRegisters[ICS_BASE_REGISTER]._storage;
        if (stackFrameLimit > icsStorage.getArraySize()) {
            stop(StopReason.ICSBaseRegisterInvalid, 0);
            return;
        }

        int sx = (int)stackOffset;
        icsStorage.setWord36(sx, _programAddressRegister);
        icsStorage.setWord36(sx + 1, _designatorRegister);
        icsStorage.setWord36(sx + 2, _indicatorKeyRegister);
        icsStorage.setWord36(sx + 3, _quantumTimer);
        icsStorage.setWord36(sx + 4, interrupt.getInterruptStatusWord0());
        icsStorage.setWord36(sx + 5, interrupt.getInterruptStatusWord1());

        //TODO other stuff which needs to be preserved - IP PRM 5.1.3
        //      e.g., results of stuff that we figure out prior to generating U in Basic Mode maybe?
        //      or does it hurt anything to just regenerate that?  We /would/ need the following two lines...
        //pStack[6].setS1( m_PreservedProgramAddressRegisterValid ? 1 : 0 );
        //pStack[7].setValue( m_PreservedProgramAddressRegister.getW() );

        // Create jump history table entry
        createJumpHistoryTableEntry(_programAddressRegister.getW());

        // The bank described by B16 begins with 64 contiguous words, indexed by interrupt class (of which there are 64).
        // Each word is a Program Address Register word, containg the L,BDI,Offset of the interrupt handling routine
        // for the associated interrupt class.  Load the PAR appropriately.
        // Make sure B16 is valid before dereferencing through it.
        if (_baseRegisters[L0_BDT_BASE_REGISTER]._voidFlag) {
            stop(StopReason.L0BaseRegisterInvalid, 0);
            return;
        }

        Word36Array intStorage = _baseRegisters[L0_BDT_BASE_REGISTER]._storage;
        int intOffset = interrupt.getInterruptClass().getCode();
        if (intOffset >= icsStorage.getArraySize()) {
            stop(StopReason.InterruptHandlerOffsetOutOfRange, 0);
            return;
        }

        _programAddressRegister.setW(intStorage.getValue(intOffset));

        // Set designator register per IP PRM 5.1.5
        //  We'll set/clear Basic Mode later once we've got the interrupt handler bank
        boolean bmBaseRegSel = _designatorRegister.getBasicModeBaseRegisterSelection();
        boolean fhip = _designatorRegister.getFaultHandlingInProgress();
        _designatorRegister.clear();
        _designatorRegister.setExecRegisterSetSelected(true);
        _designatorRegister.setArithmeticExceptionEnabled(true);
        _designatorRegister.setFaultHandlingInProgress(fhip);

        if (interrupt.getInterruptClass() == MachineInterrupt.InterruptClass.HardwareCheck) {
            if (fhip) {
                stop(StopReason.InterruptHandlerHardwareFailure, 0);
                return;
            }
            _designatorRegister.setFaultHandlingInProgress(true);
        }

        // Clear the IKR and F0
        _indicatorKeyRegister.clear();
        _currentInstruction.clear();

        // Base the PAR-indicated interrupt handler bank on B0
        //TODO WE should use standard bank-manipulation algorithm here - see hardware manual 4.6.4
        byte bankLevel = (byte)_programAddressRegister.getLevel();
        short bankDescriptorIndex = (short)_programAddressRegister.getBankDescriptorIndex();
        if ((bankLevel == 0) && (bankDescriptorIndex < 32)) {
            stop(StopReason.InterruptHandlerInvalidLevelBDI, 0);
            return;
        }

        // The bank descriptor tables for bank levels 0 through 7 are described by the banks based on B16 through B23.
        // The bank descriptor will be the {n}th bank descriptor in the particular bank descriptor table,
        // where {n} is the bank descriptor index.  Read the bank descriptor into B0.
        int bankDescriptorBaseRegisterIndex = bankLevel + 16;
        if ((bankLevel < 0) || (bankLevel > 7) || _baseRegisters[bankDescriptorBaseRegisterIndex]._voidFlag) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   bankLevel,
                                                   bankDescriptorIndex);
        }

        Word36Array bdStorage = _baseRegisters[bankDescriptorBaseRegisterIndex]._storage;
        int bankDescriptorTableOffset = bankDescriptorIndex * 8;    // 8 being the size of a BD in words
        if (bankDescriptorTableOffset + 8 > bdStorage.getArraySize()) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   bankLevel,
                                                   bankDescriptorIndex);
        }

        BankDescriptor bankDescriptor = new BankDescriptor(bdStorage, bankDescriptorTableOffset);
        long bankType = bankDescriptor.getWord36(0).getS2() & 017;
        if (bankType != 0) {
            stop(StopReason.InterruptHandlerInvalidBankType, 0);
            return;
        }

        _baseRegisters[0] = new BaseRegister(bankDescriptor.getBaseAddress(),
                                             bankDescriptor.getLargeBank(),
                                             bankDescriptor.getLowerLimitNormalized(),
                                             bankDescriptor.getUpperLimitNormalized(),
                                             bankDescriptor.getAccessLock(),
                                             bankDescriptor.getGeneraAccessPermissions(),
                                             bankDescriptor.getSpecialAccessPermissions(),
                                             bdStorage);

        _designatorRegister.setBasicModeBaseRegisterSelection(bankDescriptor.getBankType() == BankDescriptor.BankType.BasicMode);
    }

    /**
     * Checks a base register to see if we can execute within it, given our current key/ring
     * <p>
     * @param baseRegister
     * <p>
     * @return
     */
    private boolean isExecuteAllowed(
        final BaseRegister baseRegister
    ) {
        return getEffectivePermissions(baseRegister)._execute;
    }

    /**
     * Checks a base register to see if we can read from it, given our current key/ring
     * <p>
     * @param baseRegister
     * <p>
     * @return
     */
    private boolean isReadAllowed(
        final BaseRegister baseRegister
    ) {
        return getEffectivePermissions(baseRegister)._read;
    }

    /**
     * Indicates whether the given offset is within the addressing limits of the bank based on the given register.
     * If the bank is void, then the offset is clearly not within limits.
     * <p>
     * @param baseRegister
     * @param offset
     * <p>
     * @return
     */
    private boolean isWithinLimits(
        final BaseRegister baseRegister,
        final int offset
    ) {
        return !baseRegister._voidFlag
               && (offset >= baseRegister._lowerLimitNormalized)
               && (offset <= baseRegister._upperLimitNormalized);
    }

    /**
     * Checks a base register to see if we can write to it, given our current key/ring
     * <p>
     * @param baseRegister
     * <p>
     * @return
     */
    private boolean isWriteAllowed(
        final BaseRegister baseRegister
    ) {
        return getEffectivePermissions(baseRegister)._write;
    }

    /**
     * Loads the program counter from the value at u, presumably as part of a jump instruction.
     * Sets the prevent PC Increment flag, since PAR.PC will have the value we want, and we don't want it
     * auto-incremented.  For Extended Mode.
     * <p>
     * @param effectiveU
     */
    private void loadProgramCounterExtendedMode(
        final int effectiveU
    ) {
        //  Update jump history table, then PAR.PC, and prevent automatic PAR.PC increment.
        createJumpHistoryTableEntry(_programAddressRegister.getW());
        _programAddressRegister.setProgramCounter(effectiveU);
        _preventProgramCounterIncrement = true;
    }

    /**
     * If no other interrupt is pending, or the new interrupt is of a higher priority,
     * set the new interrupt as the pending interrupt.  Any lower-priority interrupt is dropped or ignored.
     * <p>
     * @param interrupt
     */
    private void raiseInterrupt(
        final MachineInterrupt interrupt
    ) {
        if ((_pendingInterrupt == null)
            || (interrupt.getInterruptClass().getCode() < _pendingInterrupt.getInterruptClass().getCode())) {
            _pendingInterrupt = interrupt;
        }
    }

    /**
     * Set a storage lock for the given absolute address.
     * <p>
     * If this IP already has locks, we die horribly - this is how we avoid internal deadlocks
     * If the address is already locked by any other IP, then we wait until it is not.
     * Then we lock it to this IP.
     * <p>
     * @param absAddress
     */
    public void setStorageLock(
        final AbsoluteAddress absAddress
    ) {
        synchronized(_storageLocks) {
            assert(_storageLocks.get(this).isEmpty());
        }

        boolean done = false;
        while (!done) {
            synchronized(_storageLocks) {
                boolean okay = true;
                Iterator it = _storageLocks.entrySet().iterator();
                while (okay && it.hasNext()) {
                    Map.Entry<InstructionProcessor, HashSet<AbsoluteAddress>> pair = (Map.Entry)it.next();
                    InstructionProcessor ip = pair.getKey();
                    HashSet<AbsoluteAddress> lockedAddresses = pair.getValue();
                    if (ip != this) {
                        if (lockedAddresses.contains(absAddress)) {
                            okay = false;
                            break;
                        }
                    }
                }

                if (okay) {
                    _storageLocks.get(this).add(absAddress);
                    done = true;
                }
            }

            if (!done) {
                Thread.yield();
            }
        }
    }

    /**
     * As above, but for multiple addresses.
     * <p>
     * @param absAddresses
     */
    public void setStorageLocks(
        final AbsoluteAddress[] absAddresses
    ) {
        synchronized(_storageLocks) {
            assert(_storageLocks.get(this).isEmpty());
        }

        boolean done = false;
        while (!done) {
            synchronized(_storageLocks) {
                boolean okay = true;
                Iterator it = _storageLocks.entrySet().iterator();
                while (okay && it.hasNext()) {
                    Map.Entry<InstructionProcessor, HashSet<AbsoluteAddress>> pair = (Map.Entry)it.next();
                    InstructionProcessor ip = pair.getKey();
                    HashSet<AbsoluteAddress> lockedAddresses = pair.getValue();
                    if (ip != this) {
                        for (AbsoluteAddress checkAddress : absAddresses) {
                            if (lockedAddresses.contains(checkAddress)) {
                                okay = false;
                                break;
                            }
                        }
                    }
                }

                if (okay) {
                    for (AbsoluteAddress absAddr : absAddresses) {
                        _storageLocks.get(this).add(absAddr);
                    }
                    done = true;
                }
            }

            if (!done) {
                Thread.yield();
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Async thread entry point
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Entry point for the async Worker part of this object
     */
    @Override
    public void run(
    ) {
        LOGGER.info(String.format("InstructionProcessor worker %s Starting", getName()));
        synchronized(_storageLocks) {
            _storageLocks.put(this, new HashSet<AbsoluteAddress>());
        }

        while (!_workerTerminate) {
            // If the virtual processor is not running, then the thread does nothing other than sleep slowly
            if (!_runningFlag) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            } else {
                //  Deal with pending interrupts, or conditions which will create a new pending interrupt.
                boolean somethingDone = false;
                try {
                    //  check for pending interrupts
                    somethingDone = checkPendingInterrupts();

                    //  If we don't have an instruction in F0, fetch one.
                    if (!somethingDone && !_indicatorKeyRegister.getInstructionInF0()) {
                        fetchInstruction();
                        somethingDone = true;
                    }

                    //  Execute the instruction in F0.
                    if (!somethingDone) {
                        _midInstructionInterruptPoint = false;
                        try {
                            executeInstruction();
                        } catch (UnresolvedAddressException ex) {
                            //  This is not surprising - can happen for basic mode indirect addressing.
                            //  Update the quantum timer so we can (eventually) interrupt a long or infinite sequence.
                            _midInstructionInterruptPoint = true;
                            if (_designatorRegister.getQuantumTimerEnabled()) {
                                _quantumTimer.add(Word36.NEGATIVE_ONE.getW());
                            }
                        }

                        if (!_midInstructionInterruptPoint) {
                            // Instruction is complete.  Maybe increment PAR.PC
                            if (_preventProgramCounterIncrement) {
                                _preventProgramCounterIncrement = false;
                            } else {
                                _programAddressRegister.setProgramCounter(_programAddressRegister.getProgramCounter() + 1);
                            }

                            //  Update IKR and (maybe) the quantum timer
                            _indicatorKeyRegister.setInstructionInF0(false);
                            if (_designatorRegister.getQuantumTimerEnabled()) {
                                _quantumTimer.add(OnesComplement.negate36(_currentInstructionHandler.getQuantumTimerCharge()));
                            }

                            // Should we stop, given that we've completed an instruction?
                            if (_currentRunMode == RunMode.SingleInstruction) {
                                stop(StopReason.Debug, 0);
                            }
                        }

                        somethingDone = true;
                    }
                } catch (MachineInterrupt interrupt) {
                    raiseInterrupt(interrupt);
                }

                // End of the cycle - should we stop?
                if (_currentRunMode == RunMode.SingleCycle) {
                    stop(StopReason.Debug, 0);
                }

                if (!somethingDone) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        synchronized(_storageLocks) {
            _storageLocks.remove(this);
        }

        LOGGER.info(String.format("InstructionProcessor worker %s Terminating", getName()));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public instance methods (only for consumption by FunctionHandlers)
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Retrieves consecutive word values for double or multiple-word transfer operations (e.g., DL, LRS, etc).
     * The assumption is that this call is made for a single iteration of an instruction.  Per doc 9.2, effective
     * relative address (U) will be calculated only once; however, access checks must succeed for all accesses.
     * We presume we are retrieving from GRS or from storage - i.e., NOT allowing immediate addressing.
     * Also, we presume that we are doing full-word transfers - no partial word.
     * <p>
     * @param grsCheck true if we should check U to see if it is a GRS location
     * @param operands Where we store the resulting operands - the length of this array defines how many operands we retrieve
     * <p>
     * @throws MachineInterrupt
     * @throws UnresolvedAddressException
     */
    public void getConsecutiveOperands(
        final boolean grsCheck,
        long[] operands
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Get the relative address so we can do a grsCheck
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        incrementIndexRegisterInF0();

        //  If this is a GRS reference - we do not need to look for containing banks or validate storage limits.
        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            //  For multiple accesses, advancing beyond GRS 0177 wraps back to zero.
            //  Do accessibility checks for each GRS access
            int grsIndex = relAddress;
            for (int ox = 0; ox < operands.length; ++ox, ++grsIndex) {
                if (grsIndex == 0200) {
                    grsIndex = 0;
                }

                if (!GeneralRegisterSet.isAccessAllowed(grsIndex, _designatorRegister.getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
                }

                operands[ox] = _generalRegisterSet.getRegister(grsIndex).getW();
            }

            return;
        }

        //  Loading from storage.  We need to resolve bank references (which does some storage limits checking for us)
        try {
            //  Iterate once through to create a container of absolute addresses which we're going to need presently.
            AbsoluteAddress[] absAddresses = new AbsoluteAddress[operands.length];
            for (int ox = 0; ox < operands.length; ++ox) {
                //  If this isn't the first trip through the loop, recalculate U
                if (ox > 0) {
                    relAddress = calculateRelativeAddressForGRSOrStorage(ox);
                }

                int brIndex = findBaseRegisterIndex(relAddress, false);
                BaseRegister baseReg = _baseRegisters[brIndex];

                //  Convert relative address to absolute, check the breakpoint, then retrieve the value
                AbsoluteAddress absAddress = getAbsoluteAddress(baseReg, relAddress);
                absAddresses[ox] = absAddress;
            }

            //  Now set storage locks for all the addresses
            setStorageLocks(absAddresses);

            //  Now go retrieve the operands, since all the addresses are under storage lock.
            //  Do check for breakpoints along the way...
            for (int ox = 0; ox < operands.length; ++ox) {
                checkBreakpoint(BreakpointComparison.Read, absAddresses[ox]);
                operands[ox] = _inventoryManager.getStorageValue(absAddresses[ox]);
            }
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }
    }

    /**
     * Retrieves a reference to the GeneralRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either A0 or EA0, depending on the designator register.
     * <p>
     * @param registerIndex
     * <p>
     * @return
     */
    public GeneralRegister getExecOrUserARegister(
        final int registerIndex
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserARegisterIndex(registerIndex));
    }

    /**
     * Retrieves the GRS index of the exec or user register indicated by the register index...
     * i.e., registerIndex == 0 returns the GRS index for either A0 or EA0, depending on the designator register.
     * <p>
     * @param registerIndex
     * <p>
     * @return
     */
    public int getExecOrUserARegisterIndex(
        final int registerIndex
    ) {
        return registerIndex + (_designatorRegister.getExecRegisterSetSelected() ? GeneralRegisterSet.EA0 : GeneralRegisterSet.A0);
    }

    /**
     * Retrieves a reference to the GeneralRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either R0 or ER0, depending on the designator register.
     * <p>
     * @param registerIndex
     * <p>
     * @return
     */
    public GeneralRegister getExecOrUserRRegister(
        final int registerIndex
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserRRegisterIndex(registerIndex));
    }

    /**
     * Retrieves a reference to the IndexRegister indicated by the register index...
     * i.e., registerIndex == 0 returns either X0 or EX0, depending on the designator register.
     * <p>
     * @param registerIndex
     * <p>
     * @return
     */
    public IndexRegister getExecOrUserXRegister(
        final int registerIndex
    ) {
        return (IndexRegister)_generalRegisterSet.getRegister(getExecOrUserXRegisterIndex(registerIndex));
    }

    /**
     * It has been determined that the u (and possibly h and i) fields comprise requested data.
     * Load the value indicated in F0 (_currentInstruction) as follows:
     *      For Processor Privilege 0,1
     *          value is 24 bits for DR.11 (exec 24bit indexing enabled) true, else 18 bits
     *      For Processor Privilege 2,3
     *          value is 24 bits for FO.i set, else 18 bits
     * If F0.x is zero, the immediate value is taken from the h,i, and u fields (unsigned), and negative zero is eliminated.
     * For F0.x nonzero, the immediate value is the sum of the u field (unsigned) with the F0.x(mod) signed field.
     *      For Extended Mode, with Processor Privilege 0,1 and DR.11 set, index modifiers are 24 bits; otherwise, they are 18 bits.
     *      For Basic Mode, index modifiers are always 18 bits.
     * In either case, the value will be left alone for j-field=016, and sign-extended for j-field=017.
     * <p>
     * @return
     */
    public long getImmediateOperand(
    ) {
        boolean exec24Index = _designatorRegister.getExecutive24BitIndexingEnabled();
        int privilege = _designatorRegister.getProcessorPrivilege();
        boolean valueIs24Bits = ((privilege < 2) && exec24Index) || ((privilege > 1) && (_currentInstruction.getI() != 0));
        long value;

        if (_currentInstruction.getX() == 0) {
            //  No indexing (x-field is zero).  Value is derived from h, i, and u fields.
            //  Get the value from h,i,u, and eliminate negative zero.
            value = _currentInstruction.getHIU();
            if (value == 0777777) {
                value = 0;
            }

            if ((_currentInstruction.getJ() == 017) && ((value & 0400000) != 0)) {
                value |= 0_777777_000000l;
            }

        } else {
            //  Value is taken only from the u field, and we eliminate negative zero at this point.
            value = _currentInstruction.getU();
            if ( value == 0177777 )
                value = 0;

            //  Add the contents of Xx(m), and do index register incrementation if appropriate.
            IndexRegister xReg = getExecOrUserXRegister((int)_currentInstruction.getX());

            //  24-bit indexing?
           if (!_designatorRegister.getBasicModeEnabled() && (privilege < 2) && exec24Index) {
                //  Add the 24-bit modifier
                value = OnesComplement.add36Simple(value, xReg.getXM24());
                if (_currentInstruction.getH() != 0) {
                    xReg.incrementModifier24();
                }
            } else {
                //  Add the 18-bit modifier
                value = OnesComplement.add36Simple(value, xReg.getXM());
                if (_currentInstruction.getH() != 0) {
                    xReg.incrementModifier18();
                }
            }
        }

        //  Truncate the result to the proper size, then sign-extend if appropriate to do so.
        boolean extend = _currentInstruction.getJ() == 017;
        if (valueIs24Bits) {
            value &= 077_777777l;
            if (extend && (value & 040_000000l) != 0) {
                value |= 0_777700_000000l;
            }
        } else {
            value &= 0_777777l;
            if (extend && (value & 0_400000) != 0) {
                value |= 0_777777_000000l;
            }
        }

        return value;
    }

    /**
     * See getImmediateOperand() above.
     * This is similar, however the calculated U field is only ever 16 or 18 bits, and is never sign-extended.
     * Also, we do not rely upon j-field for anything, as that has no meaning for jump instructions.
     * <p>
     * @return
     * <p>
     * @throws MachineInterrupt
     * @throws UnresolvedAddressException
     */
    public int getJumpOperand(
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        //  The following bit is how we deal with indirect addressing for basic mode.
        //  If we are doing that, it will update the U portion of the current instruction with new address information,
        //  then throw UnresolvedAddressException which will eventually route us back through here again, but this
        //  time with new address info (in reladdress), and we keep doing this until we're not doing indirect addressing.
        //  We never actually use the result of findBaseRegisterIndex...
        findBaseRegisterIndex(relAddress, false);
        incrementIndexRegisterInF0();
        return relAddress;
    }

    /**
     * The general case of retrieving an operand, including all forms of addressing and partial word access.
     * Instructions which use the j-field as part of the function code will likely set allowImmediate and
     * allowPartial false.
     * <p>
     * @param grsDestination true if we are going to put this value into a GRS location
     * @param grsCheck true if we should consider GRS for addresses < 0200 for our source
     * @param allowImmediate true if we should allow immediate addressing
     * @param allowPartial true if we should do partial word transfers (presuming we are not in a GRS address)
     * <p>
     * @return
     * <p>
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public long getOperand(
        final boolean grsDestination,
        final boolean grsCheck,
        final boolean allowImmediate,
        final boolean allowPartial
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int jField = (int)_currentInstruction.getJ();
        if (allowImmediate) {
            //  j-field is U or XU? If so, get the value from the instruction itself (immediate addressing)
            if (jField >= 016) {
                return getImmediateOperand();
            }
        }

        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        //  Loading from GRS?  If so, go get the value.
        //  If grsDestination is true, get the full value. Otherwise, honor j-field for partial-word transfer.
        //  See hardware guide section 4.3.2 - any GRS-to-GRS transfer is full-word, regardless of j-field.
        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            incrementIndexRegisterInF0();

            //  First, do accessibility checks
            if (!GeneralRegisterSet.isAccessAllowed(relAddress, _designatorRegister.getProcessorPrivilege(), false)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
            }

            //  If we are GRS or not allowing partial word transfers, do a full word.
            //  Otherwise, honor partial word transfering.
            if (grsDestination || !allowPartial) {
                return _generalRegisterSet.getRegister(relAddress).getW();
            } else {
                boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
                return extractPartialWord(_generalRegisterSet.getRegister(relAddress).getW(), jField, qWordMode);
            }
        }

        //  Loading from storage.  Do so, then (maybe) honor partial word handling.
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        incrementIndexRegisterInF0();

        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);

        checkBreakpoint(BreakpointComparison.Read, absAddress);
        try {
            long value = _inventoryManager.getStorageValue(absAddress);
            if (allowPartial) {
                boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
                value = extractPartialWord(value, jField, qWordMode);
            }

            return value;
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }
    }

    /**
     * Retrieves a partial-word operand from storage, depending upon the values of jField and quarterWordMode.
     * This is never a GRS reference, nor immediate (nor a jump or shift, for that matter).
     * <p>
     * @param jField not necessarily from j-field, this indicates the partial word to be stored
     * @param quarterWordMode needs to be set true for storing quarter words
     * <p>
     * @return
     * <p>
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public long getPartialOperand(
        final int jField,
        final boolean quarterWordMode
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        incrementIndexRegisterInF0();

        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);

        checkBreakpoint(BreakpointComparison.Read, absAddress);
        try {
            long value = _inventoryManager.getStorageValue(absAddress);
            return extractPartialWord(value, jField, quarterWordMode);
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }
    }

    /**
     * Increments the register indicated by the current instruction (F0) appropriately.
     * Only effective if f.x is non-zero.
     */
    public void incrementIndexRegisterInF0(
    ) {
        if ((_currentInstruction.getX() != 0) && (_currentInstruction.getH() != 0)) {
            IndexRegister iReg = getExecOrUserXRegister((int)_currentInstruction.getX());
            if (!_designatorRegister.getBasicModeEnabled()
                    && (_designatorRegister.getExecutive24BitIndexingEnabled())
                    && (_designatorRegister.getProcessorPrivilege() < 2)) {
                iReg.incrementModifier24();
            } else {
                iReg.incrementModifier18();
            }
        }
    }

    /**
     * The general case of incrementing an operand by some value, including all forms of addressing and partial word access.
     * Instructions which use the j-field as part of the function code will likely set allowPartial false.
     * Sets carry and overflow designators if appropriate.
     * <p>
     * @param grsCheck true if we should consider GRS for addresses < 0200 for our source
     * @param allowPartial true if we should do partial word transfers (presuming we are not in a GRS address)
     * @param incrementValue how much we increment storage by - positive or negative, but always ones-complement
     * @param twosComplement true to use twos-complement arithmetic - otherwise use ones-complement
     * <p>
     * @return true if either the starting or ending value of the operand is +/- zero
     * <p>
     * @throws MachineInterrupt if any interrupt needs to be raised.
     *                          In this case, the instruction is incomplete and should be retried if appropriate.
     * @throws UnresolvedAddressException if address resolution is unfinished (such as can happen in Basic Mode with
     *                                    Indirect Addressing).  In this case, caller should call back here again after
     *                                    checking for any pending interrupts.
     */
    public boolean incrementOperand(
        final boolean grsCheck,
        final boolean allowPartial,
        final long incrementValue,
        final boolean twosComplement
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int jField = (int)_currentInstruction.getJ();
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        //  Loading from GRS?  If so, go get the value.
        //  If grsDestination is true, get the full value. Otherwise, honor j-field for partial-word transfer.
        //  See hardware guide section 4.3.2 - any GRS-to-GRS transfer is full-word, regardless of j-field.
        boolean result = false;
        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            incrementIndexRegisterInF0();

            //  This is a GRS address.  Do accessibility checks
            if (!GeneralRegisterSet.isAccessAllowed(relAddress, _designatorRegister.getProcessorPrivilege(), false)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
            }

            //  Ignore partial-word transfers.
            GeneralRegister reg = _generalRegisterSet.getRegister(relAddress);
            if (twosComplement) {
                long sum = reg.getW();
                if (sum == 0) {
                    result = true;
                }
                sum += OnesComplement.getNative36(incrementValue);
                if (sum == 0) {
                    result = true;
                }

                reg.setW(sum);
                _designatorRegister.setCarry(false);
                _designatorRegister.setOverflow(false);
            } else {
                long sum = reg.getW();
                result = OnesComplement.isZero36(sum);
                OnesComplement.Add36Result ocResult = new OnesComplement.Add36Result();
                OnesComplement.add36(sum, incrementValue, ocResult);
                if (OnesComplement.isZero36(ocResult._sum)) {
                    result = true;
                }

                reg.setW(ocResult._sum);
                _designatorRegister.setCarry(ocResult._carry);
                _designatorRegister.setOverflow(ocResult._overflow);
            }

            return result;
        }

        //  Storage operand.  Maybe do partial-word addressing
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        incrementIndexRegisterInF0();

        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);
        checkBreakpoint(BreakpointComparison.Read, absAddress);

        try {
            boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
            long storageValue = _inventoryManager.getStorageValue(absAddress);
            long sum = allowPartial ? extractPartialWord(storageValue, jField, qWordMode) : storageValue;

            if (twosComplement) {
                if (sum == 0) {
                    result = true;
                }
                sum += OnesComplement.getNative36(incrementValue);
                if (sum == 0) {
                    result = true;
                }

                _designatorRegister.setCarry(false);
                _designatorRegister.setOverflow(false);
            } else {
                if (OnesComplement.isZero36(sum)) {
                    result = true;
                }
                OnesComplement.Add36Result ocResult = new OnesComplement.Add36Result();
                OnesComplement.add36(sum, incrementValue, ocResult);
                if (OnesComplement.isZero36(ocResult._sum)) {
                    result = true;
                }

                _designatorRegister.setCarry(ocResult._carry);
                _designatorRegister.setOverflow(ocResult._overflow);
                sum = ocResult._sum;
            }

            long storageResult = allowPartial ? injectPartialWord(storageValue, sum, jField, qWordMode) : sum;
            _inventoryManager.setStorageValue(absAddress, storageResult);
            return result;
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }
    }

    /**
     * Updates PAR.PC and sets the prevent-increment flag according to the given parameters.
     * Used for simple jump instructions.
     * <p>
     * @param counter
     * @param preventIncrement
     */
    public void setProgramCounter(
        final int counter,
        final boolean preventIncrement
    ) {
        this._programAddressRegister.setProgramCounter(counter);
        this._preventProgramCounterIncrement = preventIncrement;
    }

    /**
     * For handlers to set a particular register.  We do no access checking here.
     * <p>
     * @param grsIndex
     * @param value
     */
    public void setRegisterValue(
        final int grsIndex,
        final long value
    ) {
        _generalRegisterSet.setRegister(grsIndex, value);
    }

    /**
     * Stores consecutive word values for double or multiple-word transfer operations (e.g., DS, SRS, etc).
     * The assumption is that this call is made for a single iteration of an instruction.  Per doc 9.2, effective
     * relative address (U) will be calculated only once; however, access checks must succeed for all accesses.
     * We presume that we are doing full-word transfers - no partial word.
     * <p>
     * @param grsCheck true if we should check U to see if it is a GRS location
     * @param operands The operands to be stored
     * <p>
     * @throws MachineInterrupt
     * @throws UnresolvedAddressException
     */
    public void storeConsecutiveOperands(
        final boolean grsCheck,
        long[] operands
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  Get the first relative address so we can do a grsCheck
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            incrementIndexRegisterInF0();

            //  For multiple accesses, advancing beyond GRS 0177 wraps back to zero.
            //  Do accessibility checks for each GRS access
            int grsIndex = relAddress;
            for (int ox = 0; ox < operands.length; ++ox, ++grsIndex) {
                if (grsIndex == 0200) {
                    grsIndex = 0;
                }

                if (!GeneralRegisterSet.isAccessAllowed(grsIndex, _designatorRegister.getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
                }

                _generalRegisterSet.setRegister(grsIndex, operands[ox]);
            }

            return;
        }

        //  Storing to storage.
        incrementIndexRegisterInF0();
        try {
            //  Iterate once through to create a container of absolute addresses which we're going to need presently.
            AbsoluteAddress[] absAddresses = new AbsoluteAddress[operands.length];
            for (int ox = 0; ox < operands.length; ++ox) {
                //  If this isn't the first trip through the loop, recalculate U
                if (ox > 0) {
                    relAddress = calculateRelativeAddressForGRSOrStorage(ox);
                }

                int brIndex = findBaseRegisterIndex(relAddress, false);
                BaseRegister baseReg = _baseRegisters[brIndex];

                //  Convert relative address to absolute, check the breakpoint, then retrieve the value
                absAddresses[ox] = getAbsoluteAddress(baseReg, relAddress);
            }

            //  Now set storage locks for all the addresses
            setStorageLocks(absAddresses);

            //  Now go store the operands, since all the addresses are under storage lock.
            //  Do check for breakpoints along the way...
            for (int ox = 0; ox < operands.length; ++ox) {
                checkBreakpoint(BreakpointComparison.Read, absAddresses[ox]);
                _inventoryManager.setStorageValue(absAddresses[ox], operands[ox]);
            }
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }
    }

    /**
     * General case of storing an operand either to storage or to a GRS location
     * <p>
     * @param grsSource true if the value came from a register, so we know whether we need to ignore partial-word transfers
     * @param grsCheck true if relative addresses < 0200 should be considered GRS locations
     * @param checkImmediate true if we should consider j-fields 016 and 017 as immediate addressing (and throw away the operand)
     * @param allowPartial true if we should allow partial-word transfers (subject to GRS-GRS transfers)
     * @param operand value to be stored (36 bits significant)
     * <p>
     * @throws MachineInterrupt
     * @throws UnresolvedAddressException
     */
    public void storeOperand(
        final boolean grsSource,
        final boolean grsCheck,
        final boolean checkImmediate,
        final boolean allowPartial,
        final long operand
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        //  If we allow immediate addressing mode and j-field is U or XU... we do nothing.
        int jField = (int)_currentInstruction.getJ();
        if ((checkImmediate) && (jField >= 016)) {
            return;
        }

        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        if ((grsCheck)
                && ((_designatorRegister.getBasicModeEnabled()) || (_currentInstruction.getB() == 0))
                && (relAddress < 0200)) {
            incrementIndexRegisterInF0();

            //  First, do accessibility checks
            if (!GeneralRegisterSet.isAccessAllowed(relAddress, _designatorRegister.getProcessorPrivilege(), true)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, true);
            }

            //  If we are GRS or not allowing partial word transfers, do a full word.
            //  Otherwise, honor partial word transfer.
            if (!grsSource && allowPartial) {
                boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
                long originalValue = _generalRegisterSet.getRegister(relAddress).getW();
                long newValue = injectPartialWord(originalValue, operand, jField, qWordMode);
                _generalRegisterSet.setRegister(relAddress, newValue);
            } else {
                _generalRegisterSet.setRegister(relAddress, operand);
            }

            return;
        }

        //  This is going to be a storage thing...
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, true);
        incrementIndexRegisterInF0();

        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);

        checkBreakpoint(BreakpointComparison.Write, absAddress);
        try {
            if (allowPartial) {
                boolean qWordMode = _designatorRegister.getQuarterWordModeEnabled();
                long originalValue = _inventoryManager.getStorageValue(absAddress);
                long newValue = injectPartialWord(originalValue, operand, jField, qWordMode);
                _inventoryManager.setStorageValue(absAddress, newValue);
            } else {
                _inventoryManager.setStorageValue(absAddress, operand);
            }
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
        }
    }

    /**
     * Stores the right-most bits of an operand to a partial word in storage.
     * <p>
     * @param operand value to be stored (up to 36 bits significant)
     * @param jField not necessarily from j-field, this indicates the partial word to be stored
     * @param quarterWordMode needs to be set true for storing quarter words
     * <p>
     * @throws MachineInterrupt
     * @throws UnresolvedAddressException
     */
    public void storePartialOperand(
        final long operand,
        final int jField,
        final boolean quarterWordMode
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);

        int baseRegisterIndex = findBaseRegisterIndex(relAddress, true);
        incrementIndexRegisterInF0();

        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);

        checkBreakpoint(BreakpointComparison.Write, absAddress);
        try {
            long originalValue = _inventoryManager.getStorageValue(absAddress);
            long newValue = injectPartialWord(originalValue, operand, jField, quarterWordMode);
            _inventoryManager.setStorageValue(absAddress, newValue);
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
        }
    }

    /**
     * Updates S1 of a lock word under storage lock.
     * Does *NOT* increment the x-register in F0 (if specified), even if the h-bit is set.
     * <p>
     * @param flag if true, we expect the lock to be clear, and we set it.
     *              if false, we expect the lock to be set, and we clear it.
     * <p>
     * @throws MachineInterrupt for general errors, TestAndSetInterrupt of the lock is already in the state indicated by flag
     * @throws UnresolvedAddressException
     */
    public void testAndStore(
        final boolean flag
    ) throws MachineInterrupt,
             UnresolvedAddressException {
        int relAddress = calculateRelativeAddressForGRSOrStorage(0);
        int baseRegisterIndex = findBaseRegisterIndex(relAddress, false);
        BaseRegister baseRegister = _baseRegisters[baseRegisterIndex];
        AbsoluteAddress absAddress = getAbsoluteAddress(baseRegister, relAddress);
        setStorageLock(absAddress);

        long value;
        checkBreakpoint(BreakpointComparison.Read, absAddress);
        try {
            value = _inventoryManager.getStorageValue(absAddress);
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
        }

        if (flag) {
            //  we want to set the lock, so it needs to be clear
            if ((value & 0_010000_000000) != 0) {
                throw new TestAndSetInterrupt(baseRegisterIndex, relAddress);
            }

            value = injectPartialWord(value, 01, InstructionWord.S1, false);
        } else {
            //  We want to clear the lock, so it needs to be set
            if ((value & 0_010000_000000) == 0) {
                throw new TestAndSetInterrupt(baseRegisterIndex, relAddress);
            }

            value = injectPartialWord(value, 0, InstructionWord.S1, false);
        }

        checkBreakpoint(BreakpointComparison.Write, absAddress);
        try {
            _inventoryManager.setStorageValue(absAddress, value);
        } catch (AddressLimitsException
                 | UPINotAssignedException
                 | UPIProcessorTypeException ex) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public instance methods (for real)
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * IPs have no ancestors
     * <p>
     * @param ancestor
     * <p>
     * @return
     */
    @Override
    public boolean canConnect(
        final Node ancestor
    ) {
        return false;
    }

    /**
     * For debugging
     * <p>
     * @param writer
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format(""));//TODO actually, a whole lot to do here
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Worker interface implementation
     * <p>
     * @return our node name
     */
    @Override
    public String getWorkerName(
    ) {
        return getName();
    }

    /**
     * Starts the instantiated thread
     */
    @Override
    public final void initialize(
    ) {
        _workerThread.start();
        while (!_workerThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

    /**
     * Invoked when any other node decides to signal us
     * <p>
     * @param source
     */
    @Override
    public void signal(
        final Node source
    ) {
        //  anything to do here?
    }

    /**
     * Causes the IP to skip the next instruction.  Implemented by simply incrementing the PC.
     */
    public void skipNextInstruction(
    ) {
        _programAddressRegister.setProgramCounter(_programAddressRegister.getProgramCounter() + 1);
    }

    /**
     * Starts the processor.
     * Since the worker thread is always running, this merely wakes it up so that it can resume instruction processing.
     */
    public void start(
    ) {
        synchronized(this) {
            _runningFlag = true;
            this.notify();
        }
    }

    /**
     * Stops the processor.
     * More accurately, it puts the worker thread into not-running state, such that it no longer processes instructions.
     * Rather, it will simply sleep until such time as it is placed back into running state.
     * <p>
     * This version is for stops with additional detail
     * <p>
     * @param stopReason
     * @param detail 36-bit word further describing the stop reason
     */
    public void stop(
        final StopReason stopReason,
        final long detail
    ) {
        synchronized(this) {
            if (_runningFlag) {
                _latestStopReason = stopReason;
                _latestStopDetail = detail;
                _runningFlag = false;
                System.out.println(String.format("%s Stopping:%s Detail:%o",
                                                 getName(),
                                                 stopReason.toString(),
                                                 _latestStopDetail));//TODO remove later
                LOGGER.error(String.format("%s Stopping:%s Detail:%o",
                                           getName(),
                                           stopReason.toString(),
                                           _latestStopDetail));
                this.notify();
            }
        }
    }

    /**
     * Called during config tear-down - terminate the active thread
     */
    @Override
    public void terminate(
    ) {
        _workerTerminate = true;
        synchronized(_workerThread) {
            _workerThread.notify();
        }

        while (_workerThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
            }
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Takes a 36-bit value as input, and returns a partial-word value depending upon
     * the partialWordIndicator (presumably taken from the j-field of an instruction)
     * and the quarterWordMode flag (presumably taken from the designator register).
     * <p>
     * @param source
     * @param partialWordIndicator
     * @param quarterWordMode
     * <p>
     * @return
     */
    private static long extractPartialWord(
        final long source,
        final int partialWordIndicator,
        final boolean quarterWordMode
    ) {
        switch (partialWordIndicator) {
            case InstructionWord.W:     return source & OnesComplement.BIT_MASK_36;
            case InstructionWord.H2:    return Word36.getH2(source);
            case InstructionWord.H1:    return Word36.getH1(source);
            case InstructionWord.XH2:   return Word36.getXH2(source);
            case InstructionWord.XH1:   // XH1 or Q2
                if (quarterWordMode) {
                    return Word36.getQ2(source);
                } else {
                    return Word36.getXH1(source);
                }
            case InstructionWord.T3:    // T3 or Q4
                if (quarterWordMode) {
                    return Word36.getQ4(source);
                } else {
                    return Word36.getXT3(source);
                }
            case InstructionWord.T2:    // T2 or Q3
                if (quarterWordMode) {
                    return Word36.getQ3(source);
                } else {
                    return Word36.getXT2(source);
                }
            case InstructionWord.T1:    // T1 or Q1
                if (quarterWordMode) {
                    return Word36.getQ1(source);
                } else {
                    return Word36.getXT1(source);
                }
            case InstructionWord.S6:    return Word36.getS6(source);
            case InstructionWord.S5:    return Word36.getS5(source);
            case InstructionWord.S4:    return Word36.getS4(source);
            case InstructionWord.S3:    return Word36.getS3(source);
            case InstructionWord.S2:    return Word36.getS2(source);
            case InstructionWord.S1:    return Word36.getS1(source);
        }

        return source;
    }

    /**
     * Converts a relative address to an absolute address.
     * <p>
     * @param relativeAddress
     * @param relativeAddress
     *  <p>
     * @throws MachineInterrupt
     */
    private static AbsoluteAddress getAbsoluteAddress(
        final BaseRegister baseRegister,
        final int relativeAddress
    ) throws MachineInterrupt {
        short upi = baseRegister._baseAddress._upi;
        int actualOffset = relativeAddress - baseRegister._lowerLimitNormalized;
        int offset = baseRegister._baseAddress._offset + actualOffset;
        return new AbsoluteAddress(upi, offset);
    }

    /**
     * Takes 36-bit values as original and new values, and injects the new value as a partial word of the original value
     * depending upon the partialWordIndicator (presumably taken from the j-field of an instruction).
     * <p>
     * @param originalValue original value 36-bits significant
     * @param newValue new value right-aligned in a 6, 9, 12, 18, or 36-bit significant field
     * @param partialWordIndicator corresponds to the j-field of an instruction word
     * @param quarterWordMode true to do quarter-word mode transfers, false for third-word mode
     * <p>
     * @return composite value with right-most significant bits of newValue replacing a partial word portion of the
     *          original value
     */
    private static long injectPartialWord(
        final long originalValue,
        final long newValue,
        final int partialWordIndicator,
        final boolean quarterWordMode
    ) {
        switch (partialWordIndicator) {
            case InstructionWord.W:     return newValue;
            case InstructionWord.H2:    return Word36.setH2(originalValue, newValue);
            case InstructionWord.H1:    return Word36.setH1(originalValue, newValue);
            case InstructionWord.XH2:   return Word36.setH2(originalValue, newValue);
            case InstructionWord.XH1:   // XH1 or Q2
                if (quarterWordMode) {
                    return Word36.setQ2(originalValue, newValue);
                } else {
                    return Word36.setH1(originalValue, newValue);
                }
            case InstructionWord.T3:    // T3 or Q4
                if (quarterWordMode) {
                    return Word36.setQ4(originalValue, newValue);
                } else {
                    return Word36.setT3(originalValue, newValue);
                }
            case InstructionWord.T2:    // T2 or Q3
                if (quarterWordMode) {
                    return Word36.setQ3(originalValue, newValue);
                } else {
                    return Word36.setT2(originalValue, newValue);
                }
            case InstructionWord.T1:    // T1 or Q1
                if (quarterWordMode) {
                    return Word36.setQ1(originalValue, newValue);
                } else {
                    return Word36.setT1(originalValue, newValue);
                }
            case InstructionWord.S6:    return Word36.setS6(originalValue, newValue);
            case InstructionWord.S5:    return Word36.setS5(originalValue, newValue);
            case InstructionWord.S4:    return Word36.setS4(originalValue, newValue);
            case InstructionWord.S3:    return Word36.setS3(originalValue, newValue);
            case InstructionWord.S2:    return Word36.setS2(originalValue, newValue);
            case InstructionWord.S1:    return Word36.setS1(originalValue, newValue);
        }

        return originalValue;
    }
}
