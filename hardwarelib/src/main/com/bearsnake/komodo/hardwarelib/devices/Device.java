/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.Node;
import com.bearsnake.komodo.hardwarelib.NodeCategory;

public abstract class Device extends Node {

    private boolean _isReady = false;

    public Device(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final NodeCategory getNodeCategory() { return NodeCategory.Device; }

    public abstract DeviceModel getDeviceModel();
    public abstract DeviceType getDeviceType();

    // probe() is for devices which need to respond to external conditions (such as virtual card readers)
    // this is a nice alternative to having to establish an entire thread just for this silliness.
    public abstract void probe();

    // TODO think about this - should all devices require a buffer for reads, or produce a buffer?
    //   needs to be one way or the other, for all devices
    public abstract void performIo(final IoPacket packet);

    // The following may be overridden by subclasses if/as necessary.
    public boolean isReady() { return _isReady; }
    public void setIsReady(final boolean flag) { _isReady = flag; }

    // Default action
    @Override
    public void close() {}
}
