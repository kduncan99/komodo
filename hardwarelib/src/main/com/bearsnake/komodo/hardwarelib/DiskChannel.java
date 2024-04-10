/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.logger.LogManager;

public class DiskChannel extends Channel {

    public DiskChannel(final String nodeName) {
        super(nodeName);
    }

    @Override
    public boolean canAttach(Device device) {
        return device instanceof DiskDevice;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.DiskChannel;
    }

    @Override
    public void routeIo(final int nodeIdentifier,
                        final IoPacket ioPacket) {
        if (_logIos) {
            LogManager.logTrace(_nodeName, "routeIo(%d,%s)", nodeIdentifier, ioPacket.toString());
        }
        if (!_devices.containsKey(nodeIdentifier)) {
            ioPacket.setStatus(IoStatus.DeviceIsNotAttached);
        } else {
            ((Device)_devices.get(nodeIdentifier)).startIo(ioPacket);
        }
    }
}
