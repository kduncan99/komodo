/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

public abstract class DiskDevice extends Device {

    public DiskDevice(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final DeviceType getDeviceType() {
        return DeviceType.DiskDevice;
    }

    public abstract DiskInfo getInfo();
}
