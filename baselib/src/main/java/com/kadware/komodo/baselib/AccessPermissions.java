/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * Describes a particular set of permissions which some putative user/account/whatever has for some object, such as a bank
 */
public class AccessPermissions {

    public final boolean _enter;
    public final boolean _read;
    public final boolean _write;

    /**
     * Standard constructor
     */
    public AccessPermissions(
    ) {
        _enter = false;
        _read = false;
        _write = false;
    }

    /**
     * Copy constructor
     * @param source object to be copied
     */
    public AccessPermissions(
        final AccessPermissions source
    ) {
        _enter = source._enter;
        _read = source._read;
        _write = source._write;
    }

    /**
     * Initial value constructor
     * @param enter value for enter (jump, call, etc) permission
     * @param read value for read permission
     * @param write value for write permission
     */
    public AccessPermissions(
        final boolean enter,
        final boolean read,
        final boolean write
    ) {
        _enter = enter;
        _read = read;
        _write = write;
    }

    /**
     * Alternate initial value constructor
     * @param value composite value of 3 significant bits
     *                  04:Enter enabled flag
     *                  02:Read enabled flag
     *                  01:Write enabled flag
     */
    public AccessPermissions(
        final int value
    ) {
        _enter = (value & 04) != 0;
        _read = (value & 02) != 0;
        _write = (value & 01) != 0;
    }

    /**
     * Retrieves the discrete settings as a composite value.
     * Bit2: enter flag (MSB)
     * Bit1: read flag
     * Bit0: write flag (LSB)
     * @return composite value
     */
    public byte get(
    ) {
        return (byte)((_enter ? 04 : 0) | (_read ? 02 : 0) | (_write ? 01 : 0));
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof AccessPermissions) {
            AccessPermissions ap = (AccessPermissions) obj;
            return (_enter == ap._enter) && (_read == ap._read) && (_write == ap._write);
        }
        return false;
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
