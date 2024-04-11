/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public abstract class Device extends Node {

    private boolean _isReady = false;

    public Device(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final NodeCategory getNodeCategory() { return NodeCategory.Device; }

    public abstract DeviceModel getDeviceModel();
    public abstract DeviceType getDeviceType();
    public abstract void startIo(final IoPacket packet);

    // The following may be overridden by subclasses if/as necessary.
    public boolean isReady() { return _isReady; }
    public void setIsReady(final boolean flag) { _isReady = flag; }
}
