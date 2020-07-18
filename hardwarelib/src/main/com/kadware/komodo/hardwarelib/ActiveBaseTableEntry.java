/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Represents an active base table entry.
 * It's basically a Word36 with special getters
 */
public class ActiveBaseTableEntry {

    final long _value;

    public ActiveBaseTableEntry(long value) { _value = value; }
    public ActiveBaseTableEntry(
        final int level,
        final int bankDescriptorIndex,
        final int offset
    ) {
        _value = (((long)level & 07) << 33) | (((long)bankDescriptorIndex & 077777) << 18) | (offset & 0777777);
    }

    public int getLevel() { return (int) (_value >> 33); }
    public int getBDI() { return (int) (_value >> 18) & 077777; }
    public int getLBDI() { return (int) (_value >> 18); }
    public int getSubsetOffset() { return (int) (_value & 0777777); }
}
