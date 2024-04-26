/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public enum FileType {
    Fixed(0),
    Tape(1),
    Removable(040);

    private final int _value;

    FileType(final int value) { _value = value; }

    public int getValue() { return _value; }
}
