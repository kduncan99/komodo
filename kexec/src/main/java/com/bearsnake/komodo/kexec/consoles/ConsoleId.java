/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.consoles;

import com.bearsnake.komodo.baselib.Word36;

public class ConsoleId {

    private final long _value;

    public ConsoleId(long value) {
        _value = value;
    }

    public long getValue() { return _value; }

    /**
     * Retrieves console name assuming it is stored in the value as fieldata LSJF
     */
    public String getConsoleName() {
        return Word36.toStringFromFieldata(_value);
    }

    /**
     * Retrieves an octal representation of the value
     */
    @Override
    public String toString() { return Long.toHexString(_value); }
}
