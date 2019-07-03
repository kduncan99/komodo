/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Indicates the type of the device
 */
@SuppressWarnings("Duplicates")
public enum DeviceType
{
    None(0),
    Disk(1),        //  Disk Device
    Symbiont(2),    //  Symbiont Device (printer, reader, punch, etc)
    Tape(3);        //  Tape Device

    private final int _code;

    DeviceType(int code) { _code = code; }
    public int getCode() { return _code; }

    public static DeviceType getValue(
        final int code
    ) {
        switch (code) {
            case 1:     return Disk;
            case 2:     return Symbiont;
            case 3:     return Tape;
            default:    return None;
        }
    }
}
