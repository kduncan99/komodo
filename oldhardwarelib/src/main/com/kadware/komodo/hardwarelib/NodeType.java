/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Indicates the type of the device
 */
public enum NodeType {
    None(0),
    Disk(1),        //  Disk Device
    Symbiont(2),    //  Symbiont Device (printer, reader, punch, etc)
    Tape(3);        //  Tape Device

    private final int _code;

    NodeType(int code) {
        _code = code;
    }

    public int getCode() {
        return _code;
    }

    public static NodeType getValue(
        final int code
    ) {
        return switch (code) {
            case 1 -> Disk;
            case 2 -> Symbiont;
            case 3 -> Tape;
            default -> None;
        };
    }
}
