/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Describes a particular set of permissions which some putative user/account/whatever has for some object, such as a bank
 */
public class AccessPermissions {

    private boolean _enter;
    private boolean _read;
    private boolean _write;

    public AccessPermissions() {}

    public AccessPermissions(
        final AccessPermissions source
    ) {
        _enter = source._enter;
        _read = source._read;
        _write = source._write;
    }

    public AccessPermissions(
        final int value
    ) {
        _enter = (value & 04) != 0;
        _read = (value & 02) != 0;
        _write = (value & 01) != 0;
    }

    public AccessPermissions(
        final boolean canEnter,
        final boolean canRead,
        final boolean canWrite
    ) {
        _enter = canEnter;
        _read = canRead;
        _write = canWrite;
    }

    /**
     * Retrieves the discrete settings as a composite value.
     * Bit2: enter flag (MSB)
     * Bit1: read flag
     * Bit0: write flag (LSB)
     * @return composite value
     */
    public byte get() {
        return (byte)((_enter ? 04 : 0) | (_read ? 02 : 0) | (_write ? 01 : 0));
    }

    public boolean canEnter() { return _enter; }
    public boolean canRead() { return _read; }
    public boolean canWrite() { return _write; }

    @Override
    public boolean equals(
        final Object obj
    ) {
        return (obj instanceof AccessPermissions ap)
            && (_enter == ap._enter) && (_read == ap._read) && (_write == ap._write);
    }

    @Override
    public int hashCode() {
        return (byte)((_enter ? 04 : 0) | (_read ? 02 : 0) | (_write ? 01 : 0));
    }

    @Override
    public String toString() {
        return String.format("%senter %sread %swrite",
                             _enter ? "+" : "-",
                             _read ? "+" : "-",
                             _write ? "+" : "-");
    }
}
