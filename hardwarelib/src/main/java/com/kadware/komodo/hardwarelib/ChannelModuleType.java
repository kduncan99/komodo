/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Indicates the type of the channel module
 */
@SuppressWarnings("Duplicates")
public enum ChannelModuleType {

    Byte(1),
    Word(2);

    private final int _code;

    ChannelModuleType(int code) { _code = code; }

    public int getCode() { return _code; }

    public static ChannelModuleType getValue(
        final int code
    ) {
        switch (code) {
            case 1:     return Byte;
            case 2:     return Word;
            default:    return null;
        }
    }
};
