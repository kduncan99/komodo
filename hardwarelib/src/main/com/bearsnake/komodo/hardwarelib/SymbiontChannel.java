/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.baselib.ArraySlice;

import java.nio.ByteBuffer;

/**
 * Channel for symbiont devices
 */
public class SymbiontChannel extends Channel {

    public SymbiontChannel(final String nodeName) {
        super(nodeName);
    }

    @Override
    public boolean canAttach(
        Device device
    ) {
        return device instanceof SymbiontDevice;
    }

    @Override
    public void doControl(
        final ChannelProgram channelProgram,
        final Device device
    ) {
        channelProgram.setIoStatus(IoStatus.InvalidFunction);
    }

    @Override
    public void doRead(
        final ChannelProgram channelProgram,
        final Device device
    ) {
        if (channelProgram._controlWords.size() != 1) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        var cw = channelProgram._controlWords.getFirst();
        if (cw.getBuffer() == null) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        if (cw.getDirection() != ChannelProgram.Direction.Increment) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        if (cw.getTransferCount() <= 0) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        if ((cw.getBufferOffset() < 0) || (cw.getBufferOffset() >= cw.getBuffer().getSize())) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        var totalWordCount = cw.getTransferCount();
        var ioPkt = new SymbiontIoPacket();
        ioPkt.setWordCount(totalWordCount)
             .setFunction(IoFunction.Read);
        device.startIo(ioPkt);
        if (ioPkt.getStatus() != IoStatus.Complete) {
            channelProgram.setIoStatus(ioPkt.getStatus());
            return;
        }

        var array = ArraySlice.stringToWord36ASCII(new String(ioPkt.getBuffer().array()));
        var destBuffer = cw.getBuffer();
        var sx = 0;
        var dx = 0;
        while (dx < destBuffer.getSize() && sx < array._length) {
            destBuffer.set(dx++, array.get(sx++));
        }

        channelProgram.setWordsTransferred(array._length / 4);
        channelProgram.setIoStatus(ioPkt.getStatus());
    }

    @Override
    public void doReadBackward(
        final ChannelProgram channelProgram,
        final Device device
    ) {
        channelProgram.setIoStatus(IoStatus.InvalidFunction);
    }

    @Override
    public void doWrite(
        final ChannelProgram channelProgram,
        final Device device
    ) {
        if (channelProgram._controlWords.size() != 1) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        var cw = channelProgram._controlWords.getFirst();
        if (cw.getBuffer() == null) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        if (cw.getDirection() != ChannelProgram.Direction.Increment) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        if (cw.getTransferCount() <= 0) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        if ((cw.getBufferOffset() < 0) || (cw.getBufferOffset() >= cw.getBuffer().getSize())) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        var ascii = cw.getBuffer().toASCII(false);
        var totalWordCount = cw.getTransferCount();
        var ioPkt = new SymbiontIoPacket();
        ioPkt.setWordCount(totalWordCount)
             .setBuffer(ByteBuffer.wrap(ascii.getBytes()))
             .setFunction(IoFunction.Write);
        device.startIo(ioPkt);
        if (ioPkt.getStatus() != IoStatus.Complete) {
            channelProgram.setIoStatus(ioPkt.getStatus());
            return;
        }

        channelProgram.setWordsTransferred(totalWordCount);
        channelProgram.setIoStatus(ioPkt.getStatus());
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SymbiontChannel;
    }
}
