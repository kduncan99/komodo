/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

/**
 * Nothing really different from the VirtualAddress class, but this is a specific hard-held register in the IP.
 */
public class ProgramAddressRegister extends VirtualAddress {

    //???? Need unit tests
    /**
     * Standard Constructor
     */
    public ProgramAddressRegister(
    ) {
    }

    /**
     * Initial value constructor
     * <p>
     * @param value
     */
    public ProgramAddressRegister(
        final long value
    ) {
        super(value);
    }

    /**
     * Retrieves the offset portion of the VA, which is considered the program counter for a PAR.
     * <p>
     * @return
     */
    public int getProgramCounter(
    ) {
        return (int)getOffset();
    }

    /**
     * Sets the program counter portion of this PAR
     * <p>
     * @param counter
     */
    public void setProgramCounter(
        final int counter
    ) {
        setH2(counter);
    }
}
