/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

/**
 * Describes a particular set of permissions which some putative user/account/whatever has for some object, such as a bank
 */
public class AccessPermissions {

    public final boolean _execute;
    public final boolean _read;
    public final boolean _write;

    /**
     * Standard constructor
     */
    public AccessPermissions(
    ) {
        _execute = false;
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
        _execute = source._execute;
        _read = source._read;
        _write = source._write;
    }

    /**
     * Initial value constructor
     * @param execute value for execute permission
     * @param read value for read permission
     * @param write value for write permission
     */
    public AccessPermissions(
        final boolean execute,
        final boolean read,
        final boolean write
    ) {
        _execute = execute;
        _read = read;
        _write = write;
    }

    /**
     * Alternate initial value constructor
     * @param value composite value of 3 significant bits
     *                  04:Execute enabled flag
     *                  02:Read enabled flag
     *                  01:Write enabled flag
     */
    public AccessPermissions(
        final int value
    ) {
        _execute = (value & 04) != 0;
        _read = (value & 02) != 0;
        _write = (value & 01) != 0;
    }

    /**
     * Retrieves the discrete settings as a composite value.
     * Bit2: execute flag (MSB)
     * Bit1: read flag
     * Bit0: write flag (LSB)
     * @return composite value
     */
    public byte get(
    ) {
        return (byte)((_execute ? 04 : 0) | (_read ? 02 : 0) | (_write ? 01 : 0));
    }
}
