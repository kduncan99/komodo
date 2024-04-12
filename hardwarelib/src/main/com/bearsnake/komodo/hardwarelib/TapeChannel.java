/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public class TapeChannel extends Channel {

    public TapeChannel(final String nodeName) {
        super(nodeName);
    }

    @Override
    public boolean canAttach(Device device) {
        return device instanceof TapeDevice;
    }

    @Override
    public void doRead(final ChannelProgram channelProgram,
                       final Device device) {
        // TODO
    }

    @Override
    public void doWrite(final ChannelProgram channelProgram,
                        final Device device) {
        // TODO
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.TapeChannel;
    }
}
