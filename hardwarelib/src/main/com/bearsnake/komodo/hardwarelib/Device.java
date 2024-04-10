/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public abstract class Device extends Node {

    public Device(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final NodeCategory getNodeCategory() { return NodeCategory.Device; }

    public abstract DeviceModel getDeviceModel();
    public abstract DeviceType getDeviceType();

    public abstract void startIo(final IoPacket packet);
}
