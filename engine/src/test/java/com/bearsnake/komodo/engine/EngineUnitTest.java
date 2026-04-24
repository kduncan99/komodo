/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.interrupts.HardwareCheckInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;

public abstract class EngineUnitTest implements StorageManager, InterruptHandler {

    protected Engine _engine;

    /**
     * For base register 0 and 16-31
     */
    protected void loadBaseRegister(
        final int registerNumber,
        final boolean isLargeBank,
        final int lowerLimitNormalized,
        final int upperLimitNormalized,
        final long baseAddress,
        final ArraySlice storage
    ) {
        _engine.getBaseRegister(registerNumber)
               .setIsLargeBank(isLargeBank)
               .setLimitsNormalized(false, lowerLimitNormalized, upperLimitNormalized)
               .setBaseAddress(baseAddress)
               .setStorage(storage);
    }

    /**
     * For base registers 1 through 15
     */
    protected void loadBaseRegister(
        final int registerNumber,
        final boolean isLargeBank,
        final int lowerLimitNormalized,
        final int upperLimitNormalized,
        final long baseAddress,
        final ArraySlice storage,
        final int bankLevel,
        final int bankIndex,
        final int subsetting
    ) {
        loadBaseRegister(registerNumber, isLargeBank, lowerLimitNormalized, upperLimitNormalized, baseAddress, storage);
        var abte = _engine.getActiveBaseTableEntry(registerNumber);
        abte.setBankLevel((short)bankLevel);
        abte.setBankDescriptorIndex((short)bankIndex);
        abte.setSubsetSpecification(subsetting);
    }

    // StorageManager implementation -----------------------------------------------------------------------------------------------

    private final HashMap<Integer, ArraySlice> _segments = new HashMap<>();

    @Override
    public synchronized int allocateSegment(
        final int size
    ) throws HardwareCheckInterrupt {
        if (_segments.size() == 0x7FFFFFFF) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, 0, 0);
        }
        if (size < 0) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, 0, 0);
        }

        for (int i = 0; ; ++i) {
            if (!_segments.containsKey(i)) {
                _segments.put(i, new ArraySlice(new long[size]));
                return i;
            }
        }
    }

    @Override
    public synchronized void clearSegments() {
        _segments.clear();
    }

    @Override
    public synchronized ArraySlice getSegment(
        final int segment
    ) throws HardwareCheckInterrupt {
        if (!_segments.containsKey(segment)) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        return _segments.get(segment);
    }

    @Override
    public synchronized ArraySlice getSlice(
        final int segment,
        final int offset,
        final int length
    ) throws HardwareCheckInterrupt {
        if (!_segments.containsKey(segment)) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        var slice = _segments.get(segment);
        if ((offset < 0) || (length < 0) || (offset + length > slice.getSize())) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        return slice;
    }

    /**
     * Retrieves a word from the indicated segment.
     * @param segment The segment index, from 0 to 0x7FFFFFFF.
     * @param offset The offset within the segment, from 0 to segment size - 1.
     * @return The word at the specified offset, or 0 if the segment is invalid or the offset is out of bounds.
     */
    @Override
    public synchronized long getWord(
        final int segment,
        final int offset
    ) throws HardwareCheckInterrupt {
        if (!_segments.containsKey(segment)) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        var slice = _segments.get(segment);
        if ((offset < 0) || (offset >= slice.getSize())) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        return slice.get(offset);
    }

    /**
     * Releases the indicated segment.  The segment will no longer be accessible.
     * @param segment The segment index, from 0 to 0x7FFFFFFF.
     */
    @Override
    public synchronized void releaseSegment(
        final int segment
    ) throws HardwareCheckInterrupt {
        if (!_segments.containsKey(segment)) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        _segments.remove(segment);
    }

    public synchronized void resizeSegment(
        final int segment,
        final int newSize
    ) throws HardwareCheckInterrupt {
        if (!_segments.containsKey(segment)) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        var slice = _segments.get(segment);
        asdf
    }

    /**
     * Sets a word in the indicated segment.
     * @param segment The segment index, from 0 to 0x7FFFFFFF.
     * @param offset The offset within the segment, from 0 to segment size - 1.
     * @param value The word value to set.
     */
    @Override
    public synchronized void setWord(
        final int segment,
        final int offset,
        final long value
    ) throws HardwareCheckInterrupt {
        if (!_segments.containsKey(segment)) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        var slice = _segments.get(segment);
        if ((offset < 0) || (offset >= slice.getSize())) {
            throw new HardwareCheckInterrupt(HardwareCheckInterrupt.RecoveryAction.DownIPStorageInterface, false, segment, 0);
        }
        slice.set(offset, value);
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    // Stack things

    /**
     * Creates an ICS for testing internal mode interrupts
     * @param stackLowerLimit Lower limit of the stack (usually 0, but whatever)
     * @param stackFrameSize Size of the stack frame (should be a non-zero multiple of 16)
     * @param stackSize Total size of the stack in words
     * @throws HardwareCheckInterrupt If something goes badly wrong with storage
     */
    protected void createInterruptControlStack(
        final int stackLowerLimit,
        final int stackFrameSize,
        final int stackSize
    ) throws HardwareCheckInterrupt {
        var segx = allocateSegment(stackSize);
        var upperLimit = stackLowerLimit + stackSize - 1;
        var bReg = _engine.getBaseRegister(Engine.ICS_BASE_REGISTER);
        var xReg = _engine.getExecOrUserXRegister(Engine.ICS_STACK_POINTER);

        bReg.setStorage(getSegment(segx))
            .setLimitsNormalized(false, stackLowerLimit, upperLimit)
            .setAccessLock(new AccessLock())
            .setGeneralAccessPermissions(AccessPermissions.ALL)
            .setSpecialAccessPermissions(AccessPermissions.ALL)
            .setBaseAddress(AbsoluteAddress.construct(segx, 0));

        xReg.setXI(stackFrameSize).setXM(upperLimit + 1);
    }

    /**
     * Creates an RCS for testing various functions
     * @param stackLowerLimit Lower limit of the stack (usually 0, but whatever)
     * @param stackSize Total size of the stack in words
     * @throws HardwareCheckInterrupt If something goes badly wrong with storage
     */
    protected void createReturnControlStack(
        final int stackLowerLimit,
        final int stackSize
    ) throws HardwareCheckInterrupt {
        var segx = allocateSegment(stackSize);
        var upperLimit = stackLowerLimit + stackSize - 1;
        var bReg = _engine.getBaseRegister(Engine.RCS_BASE_REGISTER);
        var xReg = _engine.getExecOrUserXRegister(Engine.RCS_STACK_POINTER);

        bReg.setStorage(getSegment(segx))
            .setLimitsNormalized(false, stackLowerLimit, upperLimit)
            .setAccessLock(new AccessLock())
            .setGeneralAccessPermissions(AccessPermissions.ALL)
            .setSpecialAccessPermissions(AccessPermissions.ALL)
            .setBaseAddress(AbsoluteAddress.construct(segx, 0));

        xReg.setXI(0).setXM(upperLimit + 1);
    }

    /**
     * Allocates a segment of memory of the specified size and creates a stack in it.
     * The indicated base register becomes the stack descriptor,
     * while the indicated index register becomes the stack pointer.
     * @param baseRegisterNumber indicates which BaseRegister is used as the stack descriptor
     * @param stackPointerRegisterNumber indicates which X register is used as the stack pointer
     * @param stackLowerLimit Lower limit of the stack (usually 0, but whatever)
     * @param stackFrameSize Default frame size (modified by U on BUY and SELL)
     * @param stackSize Total stack size
     * @throws HardwareCheckInterrupt If something goes badly wrong with storage
     */
    protected void createStack(
        final int baseRegisterNumber,
        final int stackPointerRegisterNumber,
        final int stackLowerLimit,
        final int stackFrameSize,
        final int stackSize
    ) throws HardwareCheckInterrupt {
        var segx = allocateSegment(stackSize);
        var upperLimit = stackLowerLimit + stackSize - 1;
        var bReg = _engine.getBaseRegister(baseRegisterNumber);
        var xReg = _engine.getExecOrUserXRegister(stackPointerRegisterNumber);

        bReg.setStorage(getSegment(segx))
            .setLimitsNormalized(false, stackLowerLimit, upperLimit)
            .setAccessLock(new AccessLock())
            .setGeneralAccessPermissions(AccessPermissions.ALL)
            .setSpecialAccessPermissions(AccessPermissions.ALL)
            .setBaseAddress(AbsoluteAddress.construct(segx, 0));

        xReg.setXI(stackFrameSize).setXM(upperLimit + 1);
    }

    // -----------------------------------------------------------------------------------------------------------------------------
    // Bank descriptor tables for testing
    // We manage bank descriptor tables here, creating them in storage, and providing methods for setting up configurations
    // for unit testing of functions (instructions).
    // Bank descriptor tables are identified to a the hardware by levels of 0 to 7 (which do not imply any particular
    // precedence of privilege or priority, so far as the hardware is concerned). However, the hardware has visibility to
    // a particular bank descriptor via its level, only insofar as that BDT is defined on the current content of B16-B23.
    // Thus, it is possible for many BDTs to exist; it's just that a max of eight are visible at any given point in time.
    // SO... the user's interaction with us is:
    //  Create a BDT to contain some number of BDs. We create that BDT and assign it some particular identifier.
    //      This identifier is most definitely NOT a bank level, nor a bank descriptor; it is arbitrary.
    //  Optionally load the BDT into the BaseRegister corresponding to the desired bank level.
    //  If the BDT is loaded:
    //      [ create a bank in storage ]
    //      [ create a BankDescriptor object to describe the bank in storage ]
    //      register the bank descriptor via the bank level and bank descriptor index
    // The bank can now be loaded into one of the Base Registers from B0 - B15 via the typical instructions.

    /**
     * Creates a bank of the indicated size, loading the given BankDescriptor object with the appropriate values,
     * and allocating storage for the bank, returning an ArraySlice containing the storage.
     * @return ArraySlice containing the storage for the newly created bank
     */
    public ArraySlice createBank(
        final BankType bankType,
        final int lowerLimitNormalized,
        final int bankSize,
        final BankDescriptor bd
    ) throws HardwareCheckInterrupt {
        var segx = allocateSegment(bankSize);
        bd.setBankType(bankType);
        bd.setLargeBank(false);
        bd.setGeneralFault(false);
        bd.setInactive(false);
        bd.setDisplacement(0);
        bd.setBaseAddress(AbsoluteAddress.construct(segx, 0));
        bd.setAccessLock(new AccessLock());
        bd.setGeneralAccessPermissions(AccessPermissions.ALL);
        bd.setSpecialAccessPermissions(AccessPermissions.ALL);
        bd.setLowerLimit(lowerLimitNormalized >> 9);
        bd.setUpperLimit(lowerLimitNormalized + bankSize - 1);
        return getSegment(segx);
    }

    /**
     * Creates a bank descriptor table, assigning it a unique identifier.
     * We create storage to back the BDT, and we use the corresponding segment identifier as the BDT identifier.
     * @return identifier of the newly created BDT.
     */
    public int createBankDescriptorTable(
        final int bankDescriptorCount
    ) {
        try {
            return allocateSegment(8 * bankDescriptorCount);
        } catch (HardwareCheckInterrupt e) {
            assert(false):"Caught hardware check interrupt:" + e;
            return -1;
        }
    }

    /**
     * Loads a bank descriptor table into the base register corresponding to the specified bank level.
     * This will make the BDT visible to the hardware, as the BDT at the given bankLevel.
     * @param bankDescriptorTableIdentifier The unique identifier of the BDT to load.
     * @param bankLevel The bank level to load the BDT into (this determines which BaseRegister is selected).
     */
    public void loadBankDescriptorTableToBaseRegister(
        final int bankDescriptorTableIdentifier,
        final int bankLevel
    ) {
        try {
            assert ((bankLevel >= 0) && (bankLevel <= 7));
            var segment = getSegment(bankDescriptorTableIdentifier);
            var bReg = _engine.getBaseRegister(bankLevel + 16);
            bReg.setStorage(segment);
            bReg.setSpecialAccessPermissions(AccessPermissions.NONE);
            bReg.setGeneralAccessPermissions(AccessPermissions.NONE);
            bReg.setAccessLock(new AccessLock());
            bReg.setBaseAddress(AbsoluteAddress.construct(bankDescriptorTableIdentifier, 0));
            bReg.setLimitsNormalized(false, 0, segment.getSize() | 0777);
        } catch (HardwareCheckInterrupt e) {
            assert(false):"Caught hardware check interrupt:" + e;
        }
    }

    /**
     * Registers a bank descriptor via the bank level and bank descriptor index.
     * @param bankLevel The bank level to register the descriptor into.
     * @param bankDescriptorIndex The index of the descriptor within the bank.
     * @param bankDescriptor The descriptor to register.
     */
    public void registerBankDescriptorViaLevelAndBDI(
        final int bankLevel,
        final int bankDescriptorIndex,
        final BankDescriptor bankDescriptor
    ) {
        assert ((bankLevel >= 0) && (bankLevel <= 7)):"Invalid bank level: " + bankLevel;
        var bReg = _engine.getBaseRegister(bankLevel + 16);
        assert(!bReg.isVoid()):"Base register " + (bankLevel + 16) + " is void for bank level " + bankLevel;
        assert(bankDescriptorIndex < bReg.getUpperLimitNormalized()):"BDI " + bankDescriptorIndex + " exceeds BDT size for level " + bankLevel;
        bankDescriptor.serialize(bReg.getStorage(), 8 * bankDescriptorIndex);
    }

    // Interrupt Handler implementation --------------------------------------------------------------------------------------------

    /**
     * Default handler for interrupts.
     * Implementing subclass should override this and handle any interrupts it is interested in,
     * and call back here for any others.
     */
    @Override
    public void handleInterrupt(
        final MachineInterrupt interrupt
    ) {
        System.out.println("Unhandled interrupt: " + interrupt.toString());
        _engine.halt(HaltCode.UNIT_TEST_STOP);
    }
}
