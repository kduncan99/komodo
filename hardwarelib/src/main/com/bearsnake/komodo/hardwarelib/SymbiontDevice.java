/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public abstract class SymbiontDevice extends Device {

    public SymbiontDevice(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final DeviceType getDeviceType() {
        return DeviceType.SymbiontDevice;
    }

    public abstract boolean isReady();
    public abstract void setIsReady(boolean flag);
}
