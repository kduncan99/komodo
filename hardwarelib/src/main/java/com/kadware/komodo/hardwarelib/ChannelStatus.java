/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

@SuppressWarnings("Duplicates")
public enum ChannelStatus {
    Successful(0),
    Cancelled(1),
    DeviceError(2),                     //  device status is available
    UnconfiguredChannelModule(3),       //  returned by IOP
    UnconfiguredDevice(4),
    InvalidAddress(5),
    InsufficientBuffers(6),
    InProgress(040),
    InvalidStatus(077);

    private final int _code;

    ChannelStatus(int code) { _code = code; }

    public int getCode() { return _code; }

    public static ChannelStatus getValue(
        final int code
    ) {
        switch (code) {
            case 0:     return Successful;
            case 1:     return Cancelled;
            case 2:     return DeviceError;
            case 3:     return UnconfiguredChannelModule;
            case 4:     return UnconfiguredDevice;
            case 5:     return InvalidAddress;
            case 6:     return InsufficientBuffers;
            case 040:   return InProgress;
            default:    return InvalidStatus;
        }
    }
}
