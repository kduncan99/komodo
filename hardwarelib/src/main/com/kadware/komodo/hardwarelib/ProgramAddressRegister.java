/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Nothing really different from the VirtualAddress class, but this is a specific hard-held register in the IP.
 */
public class ProgramAddressRegister {

    private long _value = 0;

    //TODO Need unit tests
    public ProgramAddressRegister()             {}
    public long get()                           { return _value; }
    public int getLBDI()                        { return (int) (_value >> 18); }
    public int getProgramCounter()              { return (int) _value & 0_777777; }
    public void set(long value)                 { _value = value & 0_777777_777777L; }
    public void setLBDI(long value)             { _value = (_value & 0_777777) | ((value & 0_777777) << 18); }
    public void setProgramCounter(long value)   { _value = (_value & 0_777777_000000L) | (value & 0_777777); }
}
