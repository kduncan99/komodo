/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

public abstract class TapeDevice extends Device {

    public TapeDevice(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final DeviceType getDeviceType() {
        return DeviceType.TapeDevice;
    }

    public abstract TapeInfo getInfo();
}
