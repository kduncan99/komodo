/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Nothing really different from the VirtualAddress class, but this is a specific hard-held register in the IP.
 */
public class ProgramAddressRegister extends VirtualAddress {

    public ProgramAddressRegister() {
        super();
    }

    public ProgramAddressRegister(final long value) {
        super(value);
    }

    public ProgramAddressRegister(
        final short level,
        final int bdi,
        final int programCounter
    ) {
        super(level, bdi, programCounter);
    }

    public int getProgramCounter() {
        return getOffset();
    }

    public static long getCompositeValue(final int level,
                                         final int bdi,
                                         final int programCounter) {
        return ((long) (level & 07) << 33) | ((long) (bdi & 077777) << 18) | (programCounter & 0777777);
    }

    public static long getCompositeValue(final boolean execFlag,
                                         final boolean levelFlag,
                                         final int bdi,
                                         final int programCounter) {
        return getCompositeValue(translateBasicToExtendedLevel(execFlag, levelFlag), bdi, programCounter);
    }
}
