/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.interrupts.HardwareCheckInterrupt;
import com.bearsnake.komodo.engine.interrupts.MachineInterrupt;

import java.util.HashMap;
import java.util.TreeMap;

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
        final AbsoluteAddress baseAddress,
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
        final AbsoluteAddress baseAddress,
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

    private HashMap<Integer, ArraySlice> _segments = new HashMap<>();

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
