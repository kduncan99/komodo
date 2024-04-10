/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

public enum NodeCategory {

    Processor(1),
    ChannelModule(2),
    //  Controller(3),  not used
    Device(4),
    InvalidCategory(077);

    private final int _code;

    NodeCategory(int code) {
        _code = code;
    }

    public int getCode() {
        return _code;
    }

    public static NodeCategory getValue(
        final int code
    ) {
        return switch (code) {
            case 1 -> Processor;
            case 2 -> ChannelModule;
            //  We do not model Control Units, but if we did, they'd be case 3
            case 4 -> Device;
            default -> InvalidCategory;
        };
    }
}
