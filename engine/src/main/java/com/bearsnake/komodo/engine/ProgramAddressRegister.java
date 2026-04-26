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

    public int getProgramCounter() {
        return getOffset();
    }

    public void incrementProgramCounter() {
        setAddress(getProgramCounter() + 1);
    }

    public ProgramAddressRegister setProgramCounter(
        final int value
    ) {
        setAddress(value & 0_777777);
        return this;
    }
}
