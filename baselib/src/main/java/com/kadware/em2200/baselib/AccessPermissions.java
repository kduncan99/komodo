/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

/**
 * Describes a particular set of permissions which some putative user/account/whatever has for some object, such as a bank
 */
public class AccessPermissions {

    private boolean _execute;
    private boolean _read;
    private boolean _write;

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
     * <p>
     * @param source
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
     * <p>
     * @param execute
     * @param read
     * @param write
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
     * <p>
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
     * Getter
     * <p>
     * @return
     */
    public boolean getExecute(
    ) {
        return _execute;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getRead(
    ) {
        return _read;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getWrite(
    ) {
        return _write;
    }

    /**
     * Retrieves the discrete settings as a composite value.
     * Bit2: execute flag (MSB)
     * Bit1: read flag
     * Bit0: write flag (LSB)
     * <p>
     * @return
     */
    public byte get(
    ) {
        return (byte)((_execute ? 04 : 0) | (_read ? 02 : 0) | (_write ? 01 : 0));
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setExecute(
        final boolean value
    ) {
        _execute = value;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setRead(
        final boolean value
    ) {
        _read = value;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setWrite(
        final boolean value
    ) {
        _write = value;
    }

    /**
     * Composite value setter (see constructor)
     * <p>
     * @param value
     */
    public void set(
        final byte value
    ) {
        _execute = (value & 04) != 0;
        _read = (value & 02) != 0;
        _write = (value & 01) != 0;
    }

    /**
     * Setter
     * <p>
     * @param permissions
     */
    public void set(
        final AccessPermissions permissions
    ) {
        _execute = permissions._execute;
        _read = permissions._read;
        _write = permissions._write;
    }
}
