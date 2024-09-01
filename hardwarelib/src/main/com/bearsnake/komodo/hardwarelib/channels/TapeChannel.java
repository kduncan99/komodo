/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.channels;

import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.devices.TapeDevice;

public class TapeChannel extends Channel {

    public TapeChannel(final String nodeName) {
        super(nodeName);
    }

    @Override
    public boolean canAttach(
        Device device
    ) {
        return device instanceof TapeDevice;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.TapeChannel;
    }

    @Override
    public void routeIo(ChannelIoPacket packet) {
        // TODO
    }
}
