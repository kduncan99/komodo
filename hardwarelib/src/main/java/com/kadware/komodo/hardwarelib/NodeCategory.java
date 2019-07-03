/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

@SuppressWarnings("Duplicates")
public enum NodeCategory {

    None(0),
    Processor(1),
    ChannelModule(2),
    Controller(3),
    Device(4);

    private final int _code;

    NodeCategory(int code) { _code = code; }
    public int getCode() { return _code; }

    public static NodeCategory getValue(
        final int code
    ) {
        switch (code) {
            case 1:     return Processor;
            case 2:     return ChannelModule;
            case 3:     return Controller;
            default:    return Device;
        }
    }
}
