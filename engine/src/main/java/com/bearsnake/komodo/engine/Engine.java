/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.functions.Function;
import com.bearsnake.komodo.engine.interrupts.AddressingExceptionInterrupt;
import com.bearsnake.komodo.engine.interrupts.BreakpointInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

import java.util.HashMap;

public class Engine {

    private final ActivityStatePacket _activityStatePacket = new ActivityStatePacket();
    private int _baseRegisterIndexForFetch = 0; // only applies to basic mode - if 0, it is not valid; otherwise it is 12:15
    private Function _cachedFunction = null;
    private boolean _preventProgramCounterUpdate = false;

    private AbsoluteAddress _breakpointAddress = null;
    private boolean _breakpointHalt = false;

    private static final HashMap<Boolean, int[]> BASE_REGISTER_CANDIDATES = new HashMap<>();
    static {
        BASE_REGISTER_CANDIDATES.put(true, new int[]{13, 15, 12, 14});
        BASE_REGISTER_CANDIDATES.put(false, new int[]{12, 14, 13, 15});
    }

    public void cycle() throws MachineInterrupt {
        if (!_activityStatePacket.getIndicatorKeyRegister().getInstructionInF0()) {
            fetchInstruction();
        }

        // TODO
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
        var bReg = _activityStatePacket.getBaseRegister(baseRegisterIndex);
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

        var bReg = _activityStatePacket.getBaseRegister(baseRegisterIndex);
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

    /**
     * Check the provided absolute address to see if it matches the breakpoint conditions.
     * If it does and we have breakpoint halt set, throw an interrupt.
     * If we do not have halt set, just return true;
     * @param breakpointMask Indicates the action being taken (usually just one BREAKPOINT_ constant will be provided)
     * @param absoluteAddress Absolute address to check
     * @return true if the breakpoint was hit, false otherwise
     * @throws BreakpointInterrupt if the breakpoint halt flag is set and the breakpoint was hit
     */
    private boolean checkBreakpoint(
        final int breakpointMask,
        final AbsoluteAddress absoluteAddress
    ) throws BreakpointInterrupt {
        if ((_breakpointAddress != null) && _breakpointAddress.equals(absoluteAddress)) {
            if (((breakpointMask & Constants.BREAKPOINT_FETCH) != 0)
                || ((breakpointMask & Constants.BREAKPOINT_READ) != 0)
                || ((breakpointMask & Constants.BREAKPOINT_WRITE) != 0)) {
                _activityStatePacket.getIndicatorKeyRegister().setBreakpointRegisterMatchCondition(true);
                if (_breakpointHalt) {
                    throw new BreakpointInterrupt();
                } else {
                    return true;
                }
            }
        }
        return false;
	}

    private void executeInstruction() {
        // TODO log instruction of logging instructions is enabled
        var designatorRegister = _activityStatePacket.getDesignatorRegister();
        var instruction = _activityStatePacket.getCurrentInstruction();
        _preventProgramCounterUpdate = false;

        // TODO
    }

    /**
     * Fetches the next instruction from memory, handling basic mode bank switching and access checks.
     */
    private void fetchInstruction()
        throws AddressingExceptionInterrupt,
               BreakpointInterrupt,
               ReferenceViolationInterrupt {
        var basicMode = _activityStatePacket.getDesignatorRegister().isBasicModeEnabled();
        var programCounter = _activityStatePacket.getProgramAddressRegister().getProgramCounter();
        BaseRegister bReg = null;
        int brx;

        if (basicMode) {
            // If we don't know the index of the current basic mode instruction bank,
            // find it and set DB31 accordingly.
            if (_baseRegisterIndexForFetch == 0) {
                brx = findBasicModeBank(programCounter);
                if (brx == 0) {
                    throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
                }
                _baseRegisterIndexForFetch = brx;
                _activityStatePacket.getDesignatorRegister().setBasicModeBaseRegisterSelection((brx == 13) || (brx == 15));
            } else {
                brx = _baseRegisterIndexForFetch;
            }

            bReg = _activityStatePacket.getBaseRegister(_baseRegisterIndexForFetch);
            if (!isReadAllowed(bReg)) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, true);
            }
        } else {
            brx = 0;
            bReg = _activityStatePacket.getBaseRegister(0);
            var ikr = _activityStatePacket.getIndicatorKeyRegister();
            checkAccessLimitsAndAccessibility(basicMode, 0, programCounter, true, false, false, new AccessKey(ikr.getAccessKey()));
        }

        if (bReg.isVoid() || bReg.getBankDescriptor().isLargeBank()) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        _activityStatePacket.getCurrentInstruction().setW(0);
        _activityStatePacket.getIndicatorKeyRegister().setInstructionInF0(true);
        _activityStatePacket.getIndicatorKeyRegister().setExecuteRepeatedInstruction(false);

        VirtualAddress va = new VirtualAddress();
        AbsoluteAddress aa = new AbsoluteAddress(0, 0, 0);
        translateAddress(brx, programCounter, va, aa);
        checkBreakpoint(Constants.BREAKPOINT_FETCH, aa);
    }

    /**
     * Given a relative address we determine which (if any) basic mode banks currently based on
     * BDR12-15 is to be selected for that address.
     * Returns the band descriptor index (from 12 to 15) for the proper bank descriptor.
     * Returns 0 if no bank is selected.
     */
    private int findBasicModeBank(final long relativeAddress) {
        // Check DB31 - if true, B13/B15 are the primary pair. When false, B12/B14 are primary.
        var db31 = _activityStatePacket.getDesignatorRegister().getBasicModeBaseRegisterSelection();
        var table = BASE_REGISTER_CANDIDATES.get(db31);
        for (int tx = 0; tx < 4; tx++) {
            //  See IP PRM 4.4.5 - select the base register from the selection table.
            //  If the bank is void, skip it.
            //  If the program counter is outside the bank limits, skip it.
            //  Otherwise, we found the BDR we want to use.
            var brIndex = table[tx];
            var bReg = _activityStatePacket.getBaseRegister(brIndex);
            if (isWithinLimits(bReg, relativeAddress)) {
                return brIndex;
            }
        }

        return 0;
    }

    /*
// incrementIndexRegisterInF0 checks the instruction and current modes to determine whether register Xx
// should be incremented, and if so it performs the appropriate incrementation.
func (e *InstructionEngine) incrementIndexRegisterInF0() {
	ci := e.GetCurrentInstruction()
	if ci.GetX() > 0 && ci.GetH() > 0 {
		xReg := e.GetExecOrUserXRegister(ci.GetX())
		dr := e.GetDesignatorRegister()
		if !dr.IsBasicModeEnabled() && (dr.GetProcessorPrivilege() < 2) && dr.IsExecutive24BitIndexingSet() {
			xReg.IncrementModifier24()
		} else {
			xReg.IncrementModifier()
		}
	}
}
*/

    /**
     * Checks whether caller is allowed read (and possibly write) access to a particular GRS register.
     * @param registerIndex GRS index of register
     * @param processorPrivilege current processor privilege level
     * @param writeAccess true if we are checking for write access
     */
    public boolean isGRSAccessAllowed(
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
    public boolean isReadAllowed(
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
     * Translates a relative address within the context of the base register indicated by the given index,
     * to a virtual address and an absolute address. This is ONLY for extended mode.
     * @param baseRegisterIndex
     * @param relativeAddress
     * @param virtualAddress
     * @param absoluteAddress
     * @throws MachineInterrupt
     */
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
            ActiveBaseTable.Entry abte = _activityStatePacket.getActiveBaseTable().getEntry(baseRegisterIndex);
            level = abte.getBankLevel();
            bdi = abte.getBankDescriptorIndex();
            offset = abte.getSubsetSpecification();
        }

        var baseRegister = _activityStatePacket.getBaseRegister(baseRegisterIndex);
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
