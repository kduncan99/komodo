/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

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
                        final ChannelProgram channelProgram) {
        // TODO
    }
}
