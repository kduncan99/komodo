/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

/**
 * Describes a particular set of permissions which some putative user/account/whatever has for some object, such as a bank
 */
public class AccessPermissions {

    public static final AccessPermissions ALL = new AccessPermissions(true, true, true);
    public static final AccessPermissions NONE = new AccessPermissions(false, false, false);

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
        fromComposite(value);
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

    public AccessPermissions clear() {
        _enter = false;
        _read = false;
        _write = false;
        return this;
    }

    public boolean canEnter() { return _enter; }
    public boolean canRead() { return _read; }
    public boolean canWrite() { return _write; }

    public void fromComposite(final int value) {
        _enter = (value & 04) != 0;
        _read = (value & 02) != 0;
        _write = (value & 01) != 0;
    }

    public AccessPermissions set(final AccessPermissions source) {
        _enter = source._enter;
        _read = source._read;
        _write = source._write;
        return this;
    }

    public AccessPermissions setCanEnter(final boolean flag) {
        _enter = flag;
        return this;
    }

    public AccessPermissions setCanRead(final boolean flag) {
        _read = flag;
        return this;
    }

    public AccessPermissions setCanWrite(final boolean flag) {
        _write = flag;
        return this;
    }

    /**
     * Retrieves the discrete settings as a composite value.
     * Bit2: enter flag (MSB)
     * Bit1: read  flag
     * Bit0: write flag (LSB)
     * @return composite value
     */
    public byte toComposite() {
        return (byte)((_enter ? 04 : 0) | (_read ? 02 : 0) | (_write ? 01 : 0));
    }

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
