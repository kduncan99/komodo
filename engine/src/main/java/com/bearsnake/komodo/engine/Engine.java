/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.InstructionWord;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.engine.exceptions.EngineException;
import com.bearsnake.komodo.engine.exceptions.EngineHaltedException;
import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.functions.FunctionTable;
import com.bearsnake.komodo.engine.interrupts.*;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * Our design specifies the possibility of using multiple Engine objects, all sharing the same memory.
 */
public class Engine {

    public static enum HaltCode {
        HLTJ_INSTRUCTION,
    }

    private static final int JUMP_HISTORY_TABLE_SIZE = 512;

    public enum InstructionPoint {
        BETWEEN_INSTRUCTIONS,
        RESOLVING_ADDRESS,
        MID_INSTRUCTION,
    }

    private final ActiveBaseTable _activeBaseTable = new ActiveBaseTable();
    private final ActivityStatePacket _activityStatePacket = new ActivityStatePacket();
    private final BaseRegister[] _baseRegisters = new BaseRegister[32];
    private final GeneralRegisterSet _generalRegisterSet = new GeneralRegisterSet();

    private HaltCode _haltCode = null;

    // Normally PC is incremented at the end of instruction execution.
    // Transfer instructions set this flag to prevent this behavior, as they have already
    // set the PC to the desired value.
    private boolean _preventProgramCounterUpdate = false;

    // This only applies to basic mode - if it is set (12:15) it indicates the base register which contains
    // the code we are currently executing. In this case, we do not try to resolve the next PAR.PC to a
    // BM bank - we use this setting. It is recalculated when an instruction causes an entry to be made to
    // the JUMP HISTORY table and results in the environment being BASIC mode.
    private int _bmCachedBaseRegisterIndex = 0; // only applies to basic mode - if 0, it is not valid; otherwise it is 12:15

    // Set true if you want to log every instruction executed.
    // Don't do this if you want good performance.
    private boolean _traceInstructions = true;

    private final long[] _jumpHistoryTable = new long[JUMP_HISTORY_TABLE_SIZE];
    private int _jumpHistoryTableFirstIndex = 0;    // index of first existing entry in the jump history table
    private int _jumpHistoryTableNextIndex = 0;     // where we put the next entry

    // ScratchPad is a collection of related data items which are manipulated during the processes of
    // developing an address for fetching, reading, and writing. They are kept here to remove the noise
    // from the more basic data items in the Engine class.
    public static class ScratchPad {
        private Function _cachedFunction;
        private InstructionPoint _instructionPoint;
        public boolean _operandIsGRS;
        public int _operandBaseRegisterIndex;
        public int _operandRelativeAddress;
    }

    public final ScratchPad _scratchpad = new ScratchPad();

    // Used while determining an appropriate bank for relative address resolution in basic mode.
    private static final HashMap<Boolean, int[]> BASE_REGISTER_CANDIDATES = new HashMap<>();
    static {
        BASE_REGISTER_CANDIDATES.put(true, new int[]{13, 15, 12, 14});
        BASE_REGISTER_CANDIDATES.put(false, new int[]{12, 14, 13, 15});
    }

    // Inventory of temporarily-locked addresses - this is used when necessary, to lock a particular
    // memory location for purposes including (but maybe not limited to) instructions which read AND write
    // to a memory location across interrupt points. It is static because it is most needed when we have
    // multiple active Engine objects.
    private static final HashMap<AbsoluteAddress, Engine> _lockedAddresses = new HashMap<>();
    private static boolean _lockIsHeldByUs = false;

    // Interrupt Stack - there may be at most one of each class of interrupt posted on the stack.
    // In practice there will rarely be more than one or two.
    // Caller must poll for interrupts before calling cycle().
    private final TreeMap<MachineInterrupt.InterruptClass, MachineInterrupt> _interruptStack = new TreeMap<>();

    public Engine() {
        IntStream.range(0, 32).forEach(bx -> _baseRegisters[bx] = BaseRegister.createVoid());
        _scratchpad._instructionPoint = InstructionPoint.BETWEEN_INSTRUCTIONS;
    }

    /**
     * Clears all the locks held by this engine.
     */
    private void addressClearLocks() {
        if (_lockIsHeldByUs) {
            synchronized (_lockedAddresses) {
                _lockedAddresses.entrySet()
                                .removeIf(entry -> entry.getValue() == this);
                _lockIsHeldByUs = false;
            }
        }
    }

    /**
     * Attempts to lock the given memory address.
     * Note that any given entity cannot lock the same address more than once.
     * @param address the address to lock
     * @return true if we could obtain the lock, false otherwise.
     */
    private boolean addressLock(
        final AbsoluteAddress address
    ) {
        synchronized (_lockedAddresses) {
            if (_lockedAddresses.containsKey(address)) {
                return false;
            } else {
                _lockedAddresses.put(address, this);
                _lockIsHeldByUs = true;
                return true;
            }
        }
    }

    /**
     * Requests a lock on the given memory address, and waits until the lock is obtained.
     * WARNING - this can wait forever if the entity which locked the address misbehaves.
     * An entity should NEVER call this if it has already locked the address.
     * @param address the address to lock
     */
    private void addressLockAndWait(
        final AbsoluteAddress address
    ) {
        while (!addressLock(address)) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                // Do nothing
            }
        }
    }

    /**
     * Checks the accessibility of a given relative address in the bank described by this
     * base register for the given flags, using the given key.
     * If the check fails, we throw an interrupt which the caller should handle appropriately.
     */
    private void checkAccessLimitsAndAccessibility(
        final boolean basicMode,
        final int baseRegisterIndex,
        final long relativeAddress,
        final boolean fetchFlag,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessKey accessKey
    ) throws ReferenceViolationInterrupt {
        checkAccessLimitsForAddress(basicMode, baseRegisterIndex, relativeAddress, fetchFlag);
        var bReg = _baseRegisters[baseRegisterIndex];
        checkAccessibility(bReg, fetchFlag, readFlag, writeFlag, accessKey);
    }

    /**
     * Checks whether the relative address is within the limits of the bank described by this base register.
     * We only need the fetch flag for posting an interrupt.
     * If the check fails, we return an interrupt which the caller should handle.
     */
    private void checkAccessLimitsForAddress(
        final boolean basicMode,
        final int baseRegisterIndex,
        final long relativeAddress,
        final boolean fetchFlag
    ) throws ReferenceViolationInterrupt {
        if (fetchFlag && relativeAddress < 0200) {
            if (basicMode || baseRegisterIndex == 0) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
            }
        }

        var bReg = _baseRegisters[baseRegisterIndex];
        if (bReg.isVoid()) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
        }

        var bDesc = bReg.getBankDescriptor();
        if ((relativeAddress < bDesc.getLowerLimitNormalized()) ||
            (relativeAddress > bDesc.getUpperLimitNormalized())) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
        }
    }

    /**
     * Checks the access limits for a consecutive range of addresses, starting at the given relativeAddress,
     * for the number of addresses. Checks for read and/or write access according to the values given
     * for readFlag and writeFlag. Uses the given access key for the determination.
     * If the check fails, we return an interrupt which the caller should post
     */
    private void checkAccessLimitsRange(
        final BaseRegister bReg,
        final long relativeAddress,
        final long addressCount,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessKey accessKey
    ) throws ReferenceViolationInterrupt {
        var bDesc = bReg.getBankDescriptor();
        if ((relativeAddress < bDesc.getLowerLimitNormalized()) ||
            ((relativeAddress + addressCount - 1) > bDesc.getUpperLimitNormalized())) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        checkAccessibility(bReg, false, readFlag, writeFlag, accessKey);
    }

    /**
     * checkAccessibility compares the given key to the lock for this base register, and determines whether
     * the requested access (fetch, read, and/or write) are allowed.
     * If the check fails, we return an interrupt which the caller should handle.
     */
    private void checkAccessibility(
        final BaseRegister bReg,
        final boolean fetchFlag,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessKey accessKey
    ) throws ReferenceViolationInterrupt {
        var perms = bReg.getEffectivePermissions(accessKey);
        if (fetchFlag && !perms.canEnter()) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
        }
        if (readFlag && !perms.canRead()) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, fetchFlag);
        }
        if (writeFlag && !perms.canWrite()) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, fetchFlag);
        }
    }

    public void clear() {
        _haltCode = null;
        _activityStatePacket.getCurrentInstruction().setW(0);
        _activityStatePacket.getDesignatorRegister().setWord36(0);
        _activityStatePacket.getIndicatorKeyRegister().setWord36(0);
        _activityStatePacket.getProgramAddressRegister().setProgramCounter(0).setBankLevel((short)0).setBankDescriptorIndex(0);
        _interruptStack.clear();
        IntStream.range(0, JUMP_HISTORY_TABLE_SIZE)
                 .forEach(i -> _jumpHistoryTable[i] = 0);
        _jumpHistoryTableFirstIndex = 0;
        _jumpHistoryTableNextIndex = 0;
        _scratchpad._instructionPoint = InstructionPoint.BETWEEN_INSTRUCTIONS;
        // TODO anything else to clear?
    }

    /**
     * For various jump-like operations, this is used to clear the cached base register index.
     */
    public void clearBMCachedBaseRegisterIndex() {
        _bmCachedBaseRegisterIndex = 0;
    }

    /**
     * Creates a Jump History Entry.
     * Since this can create a between-instructions interrupt, it should only be invoked just prior
     * to the completion of the instruction which causes it.
     */
    public void createJumpHistory(final long entry) {
        _jumpHistoryTable[_jumpHistoryTableNextIndex++] = entry;
        if (_jumpHistoryTableNextIndex == JUMP_HISTORY_TABLE_SIZE) {
            _jumpHistoryTableNextIndex = 0;
            if (_jumpHistoryTableNextIndex == _jumpHistoryTableFirstIndex) {
                postInterrupt(new JumpHistoryFullInterrupt());
                _jumpHistoryTableFirstIndex++;
                if (_jumpHistoryTableFirstIndex == JUMP_HISTORY_TABLE_SIZE) {
                    _jumpHistoryTableFirstIndex = 0;
                }
            }
        }
    }

    /**
     * DoCycle executes one cycle
     * Caller should disposition any pending interrupts before invoking this...
     * Since the engine is not specifically hardware (could be an executor for a native mode OS),
     * we don't actually know how to handle the interrupts.
     * In any event, we are driven by the following two flags in the indicator key register:
     * ---
     * INF: Indicates that a valid instruction is in F0.
     *      If we are returning from an interrupt, the instruction in F0 was interrupted.
     *      Otherwise, we are at a mid-execution point for the instruction in F0, or are doing address resolution.
     *      PAR.PC is the address of that instruction, or of an EX or EXR instruction which invoked the instruciton
     *      in F0.
     * EXRF: Indicates that F0 contains the target of an EXR instruction.
     *      If zero, PAR.PC contains the address of the next instruction to be loaded.
     *      If non-zero:
     *      	If we are returning from an interrupt, then the instruction in F0 was interrupted;
     *      	Otherwise we are at a mid-execution point for the instruction in F0, or are doing address resolution.
     *          PAR.PC will be the address of the instruction (or of an EX or EXR instruction which invoked the
     *          instruction in F0).
     * With INF == 0, we fetch the instruction referenced by PAR.PC. If that fails, INF and EXRF are still zero,
     * and interrupt is posted, and all we have to do is return to the caller so they can manage the interrupt.
     * With INF == 1 and EXRF == 0, we hand off to the instruction handler for processing.
     * With INF and EXRF == 1, we check R1 and terminate EXRF processing if R1 is zero, or else we hand off to the
     * instruction handler for processing if R1 is non-zero.
     * ---
     * When the instruction handler returns, we are in one of several possible situations:
     *  The instruction never started because of an interrupt. The interrupt will be posted by the time we get here.
     *      INF and EXRF will both be clear, and we just return to the caller and let him sort it out (see next item).
     *  The instruction is not complete, and it posted an interrupt. INF is set, EXRF may be set or clear.
     *      We should service the interrupt, and let the interrupt handler decide whether we proceed with the interrupted
     *      instruction (by restoring the activity state packet and GRS), or to abandon it (by simply never 'returning')
     *      It doesn't matter to us; we just let the caller handle the interrupts and manipulate our internals as it
     *      wishes - eventually we'll get back here ready to do the next right thing.
     *      We do not change INF or EXRF before returning to the caller.
     *      Note that, if EXRF is set, we'll get here at least once after the target instruction has been placed in F0
     *      but its U has not yet been developed. We don't care; we just keep cycling.
     *  The instruction is complete, and it was NOT the target of an EXR instruction - INF is set, EXRF is clear.
     *      It will return complete==true, interrupt==nil, and e.preventPCUpdate will be true if the instruction was
     *      a successful jump or a test that skipped NI - in both of these cases, PAR.PC has already been set
     *      appropriately. We don't have to worry about preserving the state of e.preventPCUpdate because it will only
     *      be set by the JUMP/TEST instructions just before returning complete, and we'll increment PAR.PC (or not)
     *      and clear INF before returning to the caller for potential interrupt processing.
     *  The instruction is complete, and it WAS the target of an EXR instruction (INF and EXRF are set).
     *      If the repeat register (R1) is zero, EXR processing is complete (even if we haven't yet executed the target
     *      instruction - i.e., EXR invoked with any valid target instruction with R1==0... We don't care at this point.
     *      In this case, we clear INF and EXRF, and increment PAR.PC *if* we are not prevented from doing so.
     *      If R1 is NOT zero, we simply return to the caller *without* incrementing PAR.PC even if we could have done so
     *      otherwise. Having INF and EXRF already set, we won't waste time re-evaluating the EXR instruction, we'll
     *      just (re-)execute the target instruction.
     * @return true between instructions
     * @throws EngineHaltedException if the engine is halted
     * @throws MachineInterrupt if at least one interrupt is pending (that interrupt is pulled from the stack and thrown here)
     */
    public boolean cycle()
        throws EngineHaltedException,
               MachineInterrupt {
        if (_haltCode != null) {
            throw new EngineHaltedException(_haltCode);
        } else if (!_interruptStack.isEmpty()) {
            throw _interruptStack.pollFirstEntry().getValue();
        }

        var dr = _activityStatePacket.getDesignatorRegister();
        var ikr = _activityStatePacket.getIndicatorKeyRegister();
        var par = _activityStatePacket.getProgramAddressRegister();
        var r1Reg = getExecOrUserRRegister(1);
        boolean complete = false;
        boolean isEXRF = ikr.isExecuteRepeatedInstruction();

        try {
            if (!ikr.getInstructionInF0()) {
                fetchInstruction();
            }

            // pre-cycle check for EXR - was it executed with R1 already zero?
            // If so, we're done before we even do a single cycle of it.
            if (isEXRF) {
                if (r1Reg.isZero()) {
                    complete = true;
                }
            }

            if (!complete) {
                complete = executeInstruction();
                if (ikr.isExecuteRepeatedInstruction()) {
                    if (isEXRF) {
                        // R1 is the repeat counter for EXR instructions.
                        // For Extended mode, use bits 12-35 as an unsigned counter.
                        // For Basic mode, use bits 18-35. In either case, decrement the register.
                        if (dr.isBasicModeEnabled()) {
                            r1Reg.decrementCounter18();
                        } else {
                            r1Reg.decrementCounter24();
                        }
                    }
                }
            }

            if (complete) {
                _scratchpad._instructionPoint = InstructionPoint.BETWEEN_INSTRUCTIONS;
                _scratchpad._cachedFunction = null;
                ikr.setInstructionInF0(false);
                ikr.setExecuteRepeatedInstruction(false);
                if (!_preventProgramCounterUpdate) {
                    par.incrementProgramCounter();
                }
                addressClearLocks();
            }
        } catch (MachineInterrupt e) {
            postInterrupt(e);
        }

        return complete;
    }

    /**
     * Executes the instruction indicated by the current PAR.
     * Indicates whether the instruction is complete. It will not be complete in the following situations:
     *      An instruction was interrupted mid-point
     *      A Basic-Mode instruction with indirect addressing has not completed address resolution
     *      An EXR instruction has not completed execution
     * Returns true if the instruction was completed, false if it was not.
     */
    private boolean executeInstruction()
        throws MachineInterrupt {
        var designatorRegister = _activityStatePacket.getDesignatorRegister();
        var instruction = _activityStatePacket.getCurrentInstruction();

        if (_traceInstructions) {
            var str = Function.interpret(designatorRegister, instruction);
            // TODO log this, don't print it
            if (_scratchpad._instructionPoint == InstructionPoint.RESOLVING_ADDRESS) {
                IO.println("   [" + str + "]");
            } else {
                IO.println("--> " + str);
            }
        }

        if (_scratchpad._cachedFunction == null) {
            _scratchpad._cachedFunction = FunctionTable.lookupFunction(designatorRegister, instruction);
        }

        _preventProgramCounterUpdate = false;
        return _scratchpad._cachedFunction.execute(this);
    }

    /**
     * Enables or disables instruction tracing.
     * @param flag true to enable tracing, false to disable
     */
    public void enableTraceInstructions(
        final boolean flag
    ) {
        _traceInstructions = flag;
    }

    /**
     * Extracts a partial word from the given source value, based on the partial word indicator and quarter word mode.
     * @param source the source value to extract from
     * @param partialWordIndicator the partial word indicator, indicating which part of the word to extract
     * @param quarterWordMode whether to extract a quarter word (18 bits) or a half word (36 bits)
     * @return the extracted partial word
     */
    private long extractPartialWord(
        final long source,
        final int partialWordIndicator,
        final boolean quarterWordMode
    ) {
        return switch (partialWordIndicator) {
            case Constants.JFIELD_W -> source;
            case Constants.JFIELD_H2 -> Word36.getH2(source);
            case Constants.JFIELD_H1 -> Word36.getH1(source);
            case Constants.JFIELD_XH2 -> Word36.getXH2(source);
            case Constants.JFIELD_XH1 -> quarterWordMode ? Word36.getQ2(source) : Word36.getXH1(source);
            case Constants.JFIELD_T3 -> quarterWordMode ? Word36.getQ4(source) : Word36.getXT3(source);
            case Constants.JFIELD_T2 -> quarterWordMode ? Word36.getQ3(source) : Word36.getXT2(source);
            case Constants.JFIELD_T1 -> quarterWordMode ? Word36.getQ1(source) : Word36.getXT1(source);
            case Constants.JFIELD_S6 -> Word36.getS6(source);
            case Constants.JFIELD_S5 -> Word36.getS5(source);
            case Constants.JFIELD_S4 -> Word36.getS4(source);
            case Constants.JFIELD_S3 -> Word36.getS3(source);
            case Constants.JFIELD_S2 -> Word36.getS2(source);
            case Constants.JFIELD_S1 -> Word36.getS1(source);
            default -> 0;
        };
    }

    /**
     * Fetches the next instruction from memory, handling basic mode bank switching and access checks.
     */
    private void fetchInstruction()
        throws ReferenceViolationInterrupt {
        var basicMode = _activityStatePacket.getDesignatorRegister().isBasicModeEnabled();
        var programCounter = _activityStatePacket.getProgramAddressRegister().getProgramCounter();
        BaseRegister bReg;

        if (basicMode) {
            // If we don't have a cached brx, develop one. Usually we will, though.
            if (_bmCachedBaseRegisterIndex == 0) {
                _bmCachedBaseRegisterIndex = findBasicModeBaseRegisterIndex(programCounter, true);
            }

            bReg = _baseRegisters[_bmCachedBaseRegisterIndex];
            if (bReg.isVoid() || bReg.getBankDescriptor().isLargeBank()) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
            }
            if (!isReadAllowed(bReg)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
            }

            _activityStatePacket.getDesignatorRegister()
                                .setBasicModeBaseRegisterSelection((_bmCachedBaseRegisterIndex == 13) || (_bmCachedBaseRegisterIndex == 15));
        } else {
            // Extended Mode. Only check to ensure we don't go out of limits
            bReg = _baseRegisters[0];
            var bd = bReg.getBankDescriptor();
            if (programCounter < bd.getLowerLimitNormalized() || programCounter > bd.getUpperLimitNormalized()) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
            }
        }

        int offset = programCounter - (int)bReg.getBankDescriptor().getLowerLimitNormalized();
        _activityStatePacket.getCurrentInstruction().setW(bReg.getStorage().get(offset));
        _activityStatePacket.getIndicatorKeyRegister().setInstructionInF0(true);
        _activityStatePacket.getIndicatorKeyRegister().setExecuteRepeatedInstruction(false);
    }

    /**
     * FOR BASIC MODE ONLY
     * Takes a relative address and determines which (if any) of the basic mode banks
     * currently based on BDR12-15 is to be selected for that address.
     * @param relativeAddress relative address to be checked
     * @param isFetch true if this is part of a fetch operation
     * @return index of chosen base register
     * @throws ReferenceViolationInterrupt if the address is not within any based bank
     */
    private int findBasicModeBaseRegisterIndex(
        final int relativeAddress,
        final boolean isFetch
    ) throws ReferenceViolationInterrupt {
        var db31 = _activityStatePacket.getDesignatorRegister().getBasicModeBaseRegisterSelection();
        for (int tx = 0; tx < 4; tx++) {
            // See IP PRM 4.4.5 - select the base register from the selection table.
            // If the bank is void, skip it.
            // If the program counter is outside the bank limits, skip it.
            // Otherwise, we found the BDR we want to use.
            var brx = BASE_REGISTER_CANDIDATES.get(db31)[tx];
            var bReg = _baseRegisters[brx];
            if (isWithinLimits(bReg, relativeAddress)) {
                return brx;
            }
        }

        throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, isFetch);
    }

    /**
     * For external callers to obtain the current ASP
     */
    public ActivityStatePacket getActivityStatePacket() {
        return _activityStatePacket;
    }

    public BaseRegister getBaseRegister(
        final int registerNumber
    ) {
        if (registerNumber < 0 || registerNumber > 31) {
            // TODO throw an exception
        }
        return _baseRegisters[registerNumber];
    }

    /**
     * Retrieves one or more word values (for double- or multi-word transfer operations).
     * The assumption is that this call is made for a single iteration of an instruction.
     * Per doc 9.2, effective relative address (U) will be calculated only once;
     * however, access checks must succeed for all accesses.
     * We presume we are retrieving from GRS or from storage - i.e., NOT allowing immediate addressing.
     * Also, we presume that we are doing full-word transfers - not partial word.
     * @param grsCheck indicates we should check U to see if it is a GRS location
     * @param count number of consecutive words to be returned
     * @return an array containing the requested operands, or null if we are in the middle of indirect address resolution.
     */
    public long[] getConsecutiveOperands(
        final boolean grsCheck,
        final int count
    ) throws MachineInterrupt {
        resolveRelativeAddress(false, grsCheck, false);
        if (_scratchpad._instructionPoint == InstructionPoint.RESOLVING_ADDRESS) {
            return null;
        }

        // Is this a GRS access? If so, we have to ensure we do not go beyond the 0177 limit.
        if (_scratchpad._operandIsGRS) {
            long[] result = new long[count];
            var grsIndex = _scratchpad._operandRelativeAddress;
            if (grsIndex + count > 0200) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.GRSViolation, false);
            }
            for (int ox = 0; ox < count; ox++) {
                if (!isGRSAccessAllowed(grsIndex, _activityStatePacket.getDesignatorRegister().getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
                }
                result[ox] = _generalRegisterSet.getRegister(grsIndex).getW();
                grsIndex++;
            }
            return result;
        }

        // Storage reference. We've already checked limits and accessibility for the first word (and thus for the bank).
        // Do a quick check for the length.
        var bReg = _baseRegisters[_scratchpad._operandBaseRegisterIndex];
        var lastAddr = _scratchpad._operandRelativeAddress + count - 1;
        if (lastAddr > bReg.getBankDescriptor().getUpperLimitNormalized()) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        var offset = (int)(_scratchpad._operandRelativeAddress - bReg.getBankDescriptor().getLowerLimitNormalized());
        return IntStream.range(0, count)
                        .mapToLong(ox -> bReg.getStorage().get(offset + ox))
                        .toArray();
    }

    /**
     * For external callers to obtain the current instruction in F0.
     */
    public InstructionWord getCurrentInstruction() {
        return _activityStatePacket.getCurrentInstruction();
    }

    public DesignatorRegister getDesignatorRegister() {
        return _activityStatePacket.getDesignatorRegister();
    }

    /**
     * FOR EXTENDED MODE ONLY
     * Determines the index of the base register index indicated by the current processor privilege
     * and the values in F0.B and (possibly) F0.I.
     */
    private int getEffectiveBaseRegisterIndex() {
        if (_activityStatePacket.getDesignatorRegister().getProcessorPrivilege() < 2) {
            // Use 5 bits for register selection (allowing selection of B16-B32)
            return _activityStatePacket.getCurrentInstruction().getIB();
        } else {
            return _activityStatePacket.getCurrentInstruction().getB();
        }
    }

    public int getExecOrUserARegisterIndex(
        final int registerNumber
    ) {
        return _activityStatePacket.getDesignatorRegister().isExecRegisterSetSelected()
               ? Constants.GRS_EA0 + registerNumber : Constants.GRS_A0 + registerNumber;
    }

    public Register getExecOrUserARegister(
        final int registerNumber
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserARegisterIndex(registerNumber));
    }

    private int getExecOrUserRRegisterIndex(
        final int registerNumber
    ) {
        return _activityStatePacket.getDesignatorRegister().isExecRegisterSetSelected()
               ? Constants.GRS_ER0 + registerNumber : Constants.GRS_R0 + registerNumber;
    }

    public Register getExecOrUserRRegister(
        final int registerNumber
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserRRegisterIndex(registerNumber));
    }

    private int getExecOrUserXRegisterIndex(
        final int registerNumber
    ) {
        return _activityStatePacket.getDesignatorRegister().isExecRegisterSetSelected()
               ? Constants.GRS_EX0 + registerNumber : Constants.GRS_X0 + registerNumber;
    }

    public Register getExecOrUserXRegister(
        final int registerNumber
    ) {
        return _generalRegisterSet.getRegister(getExecOrUserXRegisterIndex(registerNumber));
    }

    public Register getGeneralRegister(
        final int registerNumber
    ) {
        return _generalRegisterSet.getRegister(registerNumber);
    }

    public HaltCode getHaltCode() {
        return _haltCode;
    }

    /**
     * Retrieves an operand in the case where the u (and possibly h and i) fields
     * comprise the requested data.  This is NOT for jump instructions, which have slightly different rules.
     * Load the value indicated in F0 as follows:
     * ---
     * For Processor Privilege 0,1
     * 	value is 24 bits for DR.11 (kexec 24bit indexing enabled) true, else 18 bits
     * For Processor Privilege 2,3
     * 	value is 24 bits for FO.i set, else 18 bits
     * ---
     * If F0.x is zero, the immediate value is taken from the h, i, and u fields (unsigned), and negative zero is eliminated.
     * For F0.x nonzero, the immediate value is the sum of the u field (unsigned) with the F0.x(mod) signed field.
     * For Extended Mode, with Processor Privilege 0,1 and DR.11 set, index modifiers are 24 bits;
     *  otherwise, they are 18 bits;
     * For Basic Mode, index modifiers are always 18 bits.
     * ---
     * In either case, the value will be left alone for j-field=016, and sign-extended for j-field=017.
     */

    private long getImmediateOperand() {
        long operand;
        var ci = _activityStatePacket.getCurrentInstruction();
        var dr = _activityStatePacket.getDesignatorRegister();
        var exec24Index = dr.isExecutive24BitIndexingEnabled();
        var privilege = dr.getProcessorPrivilege();
        var valueIs24Bits = ((privilege < 2) && exec24Index) || ((privilege > 1) && (ci.getI() != 0));

        if (ci.getX() == 0) {
            // No indexing (x-field is zero).  Value is derived from h, i, and u fields.
            // Get the value from h,i,u, and eliminate negative zero.
            operand = ci.getHIU();
            if (operand == 0_777777) {
                operand = 0;
            }

            if ((ci.getJ() == 0_17) && ((operand & 0_400000) != 0)) {
                operand |= 0_777777_000000L;
            }
        } else {
            // Value is taken only from the u field, and we eliminate negative zero at this point.
            operand = ci.getU();
            if (operand == 0_177777) {
                operand = 0;
            }

            // Add the contents of Xx(m) if F0.x is non-zero
            if (ci.getX() != 0) {
                var xReg = getExecOrUserXRegister(ci.getX());
                if (!dr.isBasicModeEnabled() && (privilege < 2) && exec24Index) {
                    operand = Word36.addSimple(operand, xReg.getXM24());
                } else {
                    operand = Word36.addSimple(operand, xReg.getXM());
                }
                incrementIndexRegisterInF0();
            }
        }

        // Truncate the result to the proper size, then sign-extend if appropriate to do so.
        var extend = ci.getJ() == 0_17;
        if (valueIs24Bits) {
            operand &= 077_777777;
            if (extend && (operand & 040_000000) != 0) {
                operand |= 0_777700_000000L;
            }
        } else {
            operand &= 0_777777;
            if (extend && (operand & 0_400000) != 0) {
                operand |= 0_777777_000000L;
            }
        }

        return operand;
    }

    /**
     * Retrieves the current InstructionPoint
     */
    public InstructionPoint getInstructionPoint() {
        return _scratchpad._instructionPoint;
    }

    /**
     * Resolve the relative address, and return *that* as the operand.
     * Also, we do not rely upon j-field for anything, as that has no meaning for jump instructions.
     * @return requested operand or zero if we are in the middle of indirect address resolution.
     */
    public long getJumpOperand() throws MachineInterrupt {
        long operand;
        var ci = _activityStatePacket.getCurrentInstruction();
        var dr = _activityStatePacket.getDesignatorRegister();
        var exec24Index = dr.isExecutive24BitIndexingEnabled();
        var privilege = dr.getProcessorPrivilege();
        var valueIs24Bits = ((privilege < 2) && exec24Index) || ((privilege > 1) && (ci.getI() != 0));

        if ((ci.getX() == 0) && (!dr.isBasicModeEnabled())) {
            // No indexing (x-field is zero and EM).  Value is derived from h, i, and u fields.
            operand = ci.getHIU();
        } else {
            // Value is taken only from the u field
            operand = ci.getU();
        }

        // Add the contents of Xx(m) if F0.x is non-zero
        if (ci.getX() != 0) {
            var xReg = getExecOrUserXRegister(ci.getX());
            if (!dr.isBasicModeEnabled() && (privilege < 2) && exec24Index) {
                operand = Word36.addSimple(operand, xReg.getXM24());
            } else {
                operand = Word36.addSimple(operand, xReg.getXM());
            }
            incrementIndexRegisterInF0();
        }

        // Truncate the result to the proper size, then sign-extend if appropriate to do so.
        var extend = ci.getJ() == 0_17;
        if (valueIs24Bits) {
            operand &= 077_777777;
            if (extend && (operand & 040_000000) != 0) {
                operand |= 0_777700_000000L;
            }
        } else {
            operand &= 0_777777;
            if (extend && (operand & 0_400000) != 0) {
                operand |= 0_777777_000000L;
            }
        }

        // Are we doing indirect addressing?
        if (dr.isBasicModeEnabled() && (ci.getI() != 0) && (dr.getProcessorPrivilege() > 1)) {
            var brx = findBasicModeBaseRegisterIndex((int) operand, false);
            // Indirect addressing is indicated - we need to go find the actual word of storage
            // and load a new XHIU from there, into the current instruction.
            var key = _activityStatePacket.getIndicatorKeyRegister()
                                          .getAccessKey();
            checkAccessLimitsAndAccessibility(true, brx, operand, true, false, false, key);
            var bReg = _baseRegisters[brx];
            var offset = (int) (operand - bReg.getBankDescriptor()
                                              .getLowerLimitNormalized());
            var value = bReg.getStorage()
                            .get(offset);
            ci.setXHIU(value);

            _scratchpad._instructionPoint = InstructionPoint.RESOLVING_ADDRESS;
            return 0;
        }

        _scratchpad._instructionPoint = InstructionPoint.MID_INSTRUCTION;
        return operand & 0_777777;
    }

    /**
     * This is the general case of retrieving an operand, including all forms of addressing
     * and partial word access. Instructions which use the j-field as part of the function code will likely set
     * allowImmediate and allowPartialWordTransfer false.
     * @param grsDestination true if we are going to put this value into a GRS location
     * @param grsCheck true if we should consider GRS for addresses < 0200 for our source
     * @param allowImmediate true if we should allow immediate addressing
     * @param allowPartialWordTransfer true if we should allow partial word access
     * @param lockStorage true if we should lock the storage location
     * @return the operand
     * @throws MachineInterrupt in any case where an interrupt is generated
     */
    public long getOperand(
        final boolean grsDestination,
        final boolean grsCheck,
        final boolean allowImmediate,
        final boolean allowPartialWordTransfer,
        final boolean lockStorage
    ) throws MachineInterrupt {
        var ci = _activityStatePacket.getCurrentInstruction();
        var dr = _activityStatePacket.getDesignatorRegister();
        var ikr = _activityStatePacket.getIndicatorKeyRegister();
        var basicMode = dr.isBasicModeEnabled();

        // immediate operand?
        var jFIeld = ci.getJ();
        if (allowImmediate && ((jFIeld == Constants.JFIELD_U) || (jFIeld == Constants.JFIELD_XU))) {
            return getImmediateOperand();
        }

        // Get the _operandRelativeAddress.
        // For BM, this also gets the _operandBaseRegister and _operandBaseRegisterIndex OR _operandIsGRS.
        resolveRelativeAddress(false, grsCheck, false);
        if (_scratchpad._instructionPoint == InstructionPoint.RESOLVING_ADDRESS) {
            return 0;
        }

        // For EM, we need to explicitly get _operandBaseRegister and _operandBaseRegisterIndex.
        if (!basicMode) {
            getEffectiveBaseRegisterIndex();
        }

        // Loading from GRS? If so, go get the value.
        // If grsDest is true, get the full value. Otherwise, honor j-field for partial-word transfer.
        // (Any GRS-to-GRS transfer is full-word, regardless of j-field)
        if (_scratchpad._operandIsGRS) {
            var operand = _generalRegisterSet.getRegister(_scratchpad._operandRelativeAddress).getW();
            if (!grsDestination && allowPartialWordTransfer) {
                operand = extractPartialWord(operand, jFIeld, dr.isQuarterWordModeEnabled());
            }
            return operand;
        }

        // Loading from storage. Do so, then (maybe) honor partial word handling.
        var key = ikr.getAccessKey();
        checkAccessLimitsAndAccessibility(basicMode,
                                          _scratchpad._operandBaseRegisterIndex,
                                          _scratchpad._operandRelativeAddress,
                                          false, true, false, key);

        var bReg = _baseRegisters[_scratchpad._operandBaseRegisterIndex];
        if (lockStorage) {
            addressLockAndWait(bReg.getBankDescriptor().getBaseAddress());
        }

        _scratchpad._operandIsGRS = false;
        var offset = (int) (_scratchpad._operandRelativeAddress - bReg.getBankDescriptor().getLowerLimitNormalized());
        var operand = bReg.getStorage().get(offset);
        if (allowPartialWordTransfer) {
            operand = extractPartialWord(operand, jFIeld, dr.isQuarterWordModeEnabled());
        }
        return operand;
    }

    public ProgramAddressRegister getProgramAddressRegister() {
        return _activityStatePacket.getProgramAddressRegister();
    }

    /**
     * Sets the halt code for the engine.
     * Subsequent invocations of cycle() will throw HaltedException until this is cleared.
     * @param haltCode
     */
    public void halt(
        final HaltCode haltCode
    ) {
        // TODO LOG THIS
        _haltCode = haltCode;
    }

    /**
     * Specifically for the NOP instruction.  We go through the process of developing U,
     * but we do not retrieve the operand therefrom. This means that we do no access checks except in the case of checking
     * for read access during indirect address resolution.
     * Returns complete == false if we are in the middle of resolving addresses,
     * or an interrupt if we fail some sort of limits or access checking.
     */
    public void ignoreOperand() throws MachineInterrupt {
        var dr = _activityStatePacket.getDesignatorRegister();
        var ikr = _activityStatePacket.getIndicatorKeyRegister();
        var basicMode = dr.isBasicModeEnabled();

        // Get the _operandRelativeAddress.
        // For BM, this also gets the _operandBaseRegister and _operandBaseRegisterIndex.
        resolveRelativeAddress(false, true, true);
        if (_scratchpad._instructionPoint == InstructionPoint.RESOLVING_ADDRESS) {
            return;
        }

        // For EM, we need to explicitly get _operandBaseRegister and _operandBaseRegisterIndex.
        if (!basicMode) {
            getEffectiveBaseRegisterIndex();
        }

        // If we're not GRS, do the following just for access checks
        if ((_scratchpad._operandRelativeAddress > 0177) || (!basicMode && (_scratchpad._operandBaseRegisterIndex > 0))) {
            var key = ikr.getAccessKey();
            checkAccessLimitsAndAccessibility(basicMode,
                                              _scratchpad._operandBaseRegisterIndex,
                                              _scratchpad._operandRelativeAddress,
                                              false, true, false, key);
        }
    }

    /**
     * Checks the current instruction and modes to determine whether register X(x)
     * should be incremented, and if so it performs the appropriate incrementation.
     */
    private void incrementIndexRegisterInF0() {
        var ci = _activityStatePacket.getCurrentInstruction();
        if (ci.getH() > 0) {
            var dr = _activityStatePacket.getDesignatorRegister();
            var xReg = getExecOrUserXRegister(ci.getX());
            if (!dr.isBasicModeEnabled() && (dr.getProcessorPrivilege() < 2) && dr.isExecutive24BitIndexingEnabled()) {
                xReg.incrementModifier24();
            } else {
                xReg.incrementModifier18();
            }
        }
    }

    /**
     * Checks whether caller is allowed read (and possibly write) access to a particular GRS register.
     * @param registerIndex GRS index of register
     * @param processorPrivilege current processor privilege level
     * @param writeAccess true if we are checking for write access
     */
    private boolean isGRSAccessAllowed(
        final int registerIndex,
        final short processorPrivilege,
        final boolean writeAccess
    ) {
        if (registerIndex < 040) {
            return true;
        } else if (registerIndex < 0100) {
            return false;
        } else if (registerIndex < 0120) {
            return true;
        } else {
            return (writeAccess && (processorPrivilege == 0)) || (!writeAccess && (processorPrivilege <= 2));
        }
    }

    /**
     * Indicates whether caller is allowed to read from the storage described by the given base register.
     * @param bReg Base register index.
     */
    private boolean isReadAllowed(
        final BaseRegister bReg
    ) {
        return bReg.getEffectivePermissions(_activityStatePacket.getIndicatorKeyRegister().getAccessKey()).canRead();
    }

    /**
     * Checks the given offset within the constraints of the given base register,
     * returning true if the offset is within those constraints, else false.
     * @param bReg base register of interest
     * @param offset offset from start of bank
     */
    private boolean isWithinLimits(
        final BaseRegister bReg,
        final long offset
    ) {
        return !bReg.isVoid() &&
                (offset >= bReg.getBankDescriptor().getLowerLimitNormalized()) &&
                (offset <= bReg.getBankDescriptor().getUpperLimitNormalized());
    }

    /**
     * Polls to see if an interrupt is pending
     * @return the highest-priority interrupt currently pending, or null if none are pending
     */
    public MachineInterrupt pollInterrupt() {
        synchronized (_interruptStack) {
            var entry = _interruptStack.pollFirstEntry();
            return entry == null ? null : entry.getValue();
        }
    }

    /**
     * Posts an interrupt to be processed by the interrupt handler.
     */
    public void postInterrupt(
        final MachineInterrupt interrupt
    ) {
        synchronized (_interruptStack) {
            _interruptStack.put(interrupt.getInterruptClass(), interrupt);
        }
    }

    public void preventPCUpdate() {
        _preventProgramCounterUpdate = true;
    }

    /**
     * Wrapper for the two methods which provide this service for basic and extended modes, respectively.
     */
    private void resolveRelativeAddress(
        final boolean useU,
        final boolean grsCheck,
        final boolean ignoreAccessChecks
    ) throws MachineInterrupt {
        if (_activityStatePacket.getDesignatorRegister().isBasicModeEnabled()) {
            resolveBasicModeRelativeAddress(useU, grsCheck, ignoreAccessChecks);
        } else {
            resolveExtendedModeRelativeAddress(useU, grsCheck, ignoreAccessChecks);
        }
    }

    /**
     * FOR BASIC MODE ONLY
     * Reads the instruction in F0, and in conjunction with the current ASP environment,
     * develops the relative address as a function of the unsigned 16-bit U or the 12-bit D field,
     * added with the signed modifier portion of the index register indicated by F0.x (presuming that field is not zero).
     * If we hit an access or limits check during indirect address resolution, we propagate the interrupt.
     * If we have iterated on indirect address resolution, we set
     *      _instructionPoint to RESOLVING_ADDRESS.
     * Otherwise, we set
     *      _operandRelativeAddress to the calculated relative address
     *      _operandBaseRegisterIndex to indicate the containing base register
     *      _instructionPoint to MID_INSTRUCTION.
     * @param useU indicates an Extended Mode Jump instruction which uses the entire U (or HIU) fields for the relative address.
     *             basic mode always uses the u field.
     * @param grsCheck indicates whether to perform GRS access checks
     * @param ignoreAccessChecks indicates whether to ignore access checks during address resolution (for ignoreOperand())
     */
    private void resolveBasicModeRelativeAddress(
        final boolean useU,
        final boolean grsCheck,
        final boolean ignoreAccessChecks
    ) throws MachineInterrupt {
        var ci = _activityStatePacket.getCurrentInstruction();
        var dr = _activityStatePacket.getDesignatorRegister();

        int relAddr = (dr.isBasicModeEnabled() || useU) ? ci.getU() : ci.getD();
        var x = ci.getX();
        if (x != 0) {
            long addend;
            var xReg = getExecOrUserXRegister(x);
            if (dr.isExecutive24BitIndexingEnabled() && dr.getProcessorPrivilege() < 2) {
                addend = xReg.getSignedXM24();
            } else {
                addend = xReg.getSignedXM();
            }
            relAddr = (int)Word36.addSimple(relAddr, addend);
            incrementIndexRegisterInF0();
        }

        if (grsCheck && (relAddr < 0200)) {
            // GRS address
            if (!ignoreAccessChecks) {
                if (isGRSAccessAllowed(relAddr, dr.getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
                }
            }

            _scratchpad._operandIsGRS = true;
            _scratchpad._operandBaseRegisterIndex = 0;
            _scratchpad._operandRelativeAddress = relAddr;
            _scratchpad._instructionPoint = InstructionPoint.MID_INSTRUCTION;
            return;
        }

        var brx = findBasicModeBaseRegisterIndex(relAddr, false);
        if (ci.getI() != 0 && dr.getProcessorPrivilege() > 1) {
            // Indirect addressing is indicated - we need to go find the actual word of storage
            // and load a new XHIU from there, into the current instruction.
            var key = _activityStatePacket.getIndicatorKeyRegister()
                                          .getAccessKey();
            checkAccessLimitsAndAccessibility(true, brx, relAddr, true, false, false, key);
            var bReg = _baseRegisters[brx];
            var offset = (int) (relAddr - bReg.getBankDescriptor()
                                              .getLowerLimitNormalized());
            var value = bReg.getStorage()
                            .get(offset);
            ci.setXHIU(value);

            _scratchpad._instructionPoint = InstructionPoint.RESOLVING_ADDRESS;
            return;
        }

        _scratchpad._operandBaseRegisterIndex = brx;
        _scratchpad._operandRelativeAddress = relAddr;
        _scratchpad._instructionPoint = InstructionPoint.MID_INSTRUCTION;
    }

    /**
     * FOR EXTENDED MODE ONLY
     * Reads the instruction in F0, and in conjunction with the current ASP environment,
     * develops the relative address as a function of the unsigned 16-bit U or the 12-bit D field,
     * added with the signed modifier portion of the index register indicated by F0.x (presuming that field is not zero).
     * We return with
     *      _operandRelativeAddress to the calculated relative address
     *      _operandBaseRegisterIndex to indicate the containing base register
     *      _instructionPoint to MID_INSTRUCTION.
     * @param useU indicates an Extended Mode Jump instruction which uses the entire U (or HIU) fields for the relative address.
     *             basic mode always uses the u field.
     * @param grsCheck indicates whether to perform GRS access checks
     * @param ignoreAccessChecks indicates whether to ignore access checks during address resolution (for ignoreOperand())
     */
    private void resolveExtendedModeRelativeAddress(
        final boolean useU,
        final boolean grsCheck,
        final boolean ignoreAccessChecks
    ) throws ReferenceViolationInterrupt {
        var ci = _activityStatePacket.getCurrentInstruction();
        var dr = _activityStatePacket.getDesignatorRegister();

        int relAddr = (dr.isBasicModeEnabled() || useU) ? ci.getU() : ci.getD();
        var x = ci.getX();
        if (x != 0) {
            long addend;
            var xReg = getExecOrUserXRegister(x);
            if (dr.isExecutive24BitIndexingEnabled() && dr.getProcessorPrivilege() < 2) {
                addend = xReg.getSignedXM24();
            } else {
                addend = xReg.getSignedXM();
            }
            relAddr = (int)Word36.addSimple(relAddr, addend);
            incrementIndexRegisterInF0();
        }

        var brx = getEffectiveBaseRegisterIndex();
        if (grsCheck && (brx == 0) && (relAddr < 0200)) {
            // GRS address
            if (!ignoreAccessChecks) {
                if (isGRSAccessAllowed(_scratchpad._operandRelativeAddress, dr.getProcessorPrivilege(), false)) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, true);
                }
            }
            _scratchpad._operandIsGRS = true;
        }

        _scratchpad._operandBaseRegisterIndex = brx;
        _scratchpad._operandRelativeAddress = relAddr;
        _scratchpad._instructionPoint = InstructionPoint.MID_INSTRUCTION;
    }

    /**
     * Translates a relative address within the context of the base register indicated by the given index,
     * to a virtual address and an absolute address. This is ONLY for extended mode.
     * @param baseRegisterIndex
     * @param relativeAddress
     * @param virtualAddress
     * @param absoluteAddress
     * @throws MachineInterrupt
     */
    //TODO obsolete?
    private void translateAddress(
        final int baseRegisterIndex,
        final long relativeAddress,
        final VirtualAddress virtualAddress,
        final AbsoluteAddress absoluteAddress
    ) throws AddressingExceptionInterrupt {
        short level;
        int bdi;
        long offset;

        if ((baseRegisterIndex < 0) || (baseRegisterIndex > 31)) {
            // Bad index
            throw new RuntimeException("Invalid base register index");
        } else if (baseRegisterIndex == 0) {
            // B0, some things are a little different since this (in extended mode) is always the code bank.
            level = _activityStatePacket.getProgramAddressRegister().getBankLevel();
            bdi = _activityStatePacket.getProgramAddressRegister().getBankDescriptorIndex();
            offset = 0;
        } else {
            ActiveBaseTable.Entry abte = _activeBaseTable.getEntry(baseRegisterIndex);
            level = abte.getBankLevel();
            bdi = abte.getBankDescriptorIndex();
            offset = abte.getSubsetSpecification();
        }

        var baseRegister = _baseRegisters[baseRegisterIndex];
        if (baseRegister.isVoid()) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException, level, bdi);
        }

        var bankDescriptor = baseRegister.getBankDescriptor();
        offset += relativeAddress - bankDescriptor.getLowerLimitNormalized();
        virtualAddress.setBankDescriptorIndex(bdi);
        virtualAddress.setBankLevel(level);
        virtualAddress.setOffset((int)offset);

        // TODO do we need to worry about gate banks here?
        switch (bankDescriptor.getBankType()) {
            case BankType.BasicMode -> virtualAddress.translateToBasicMode();
            case BankType.ExtendedMode -> {}
            default -> throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException, level, bdi);
        }

        absoluteAddress.setUpiIndex(bankDescriptor.getBaseAddress().getUpiIndex());
        absoluteAddress.setSegment(bankDescriptor.getBaseAddress().getSegment());
        absoluteAddress.setOffset((int)(offset + bankDescriptor.getBaseAddress().getOffset()));
    }
}
