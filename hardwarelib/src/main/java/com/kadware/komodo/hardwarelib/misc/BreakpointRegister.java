/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.misc;

/**
 * Describes a base register - there are 32 of these, each describing a based bank.
 */
public class BreakpointRegister {

    private boolean _haltFlag;
    private boolean _fetchFlag;
    private boolean _readFlag;
    private boolean _writeFlag;
    private long    _absoluteAddress;   //  36 bits significant

    /**
     * Standard constructor
     */
    public BreakpointRegister(
    ) {
    }

    /**
     * Clears the values of this object
     */
    public void clear(
    ) {
        _haltFlag = false;
        _fetchFlag = false;
        _readFlag = false;
        _writeFlag = false;
        _absoluteAddress = 0;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getHaltFlag(
    ) {
        return _haltFlag;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getFetchFlag(
    ) {
        return _fetchFlag;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getReadFlag(
    ) {
        return _readFlag;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getWriteFlag(
    ) {
        return _writeFlag;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getAbsoluteAddress(
    ) {
        return _absoluteAddress;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setHaltFlag(
        final boolean value
    ) {
        _haltFlag = value;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setFetchFlag(
        final boolean value
    ) {
        _fetchFlag = value;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setReadFlag(
        final boolean value
    ) {
        _readFlag = value;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setWriteFlag(
        final boolean value
    ) {
        _writeFlag = value;
    }

    /**
     * Setter
     * <p>
     * @param value
     */
    public void setAbsoluteAddress(
        final long value
    ) {
        _absoluteAddress = value;
    }
}
