/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

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
        // TODO need to support WriteEndOfFile and StartFile
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

        var ioPkt = new SymbiontIoPacket();
        ioPkt.setFunction(IoFunction.Read);
        device.startIo(ioPkt);
        if (ioPkt.getStatus() != IoStatus.Complete) {
            channelProgram.setIoStatus(ioPkt.getStatus(), ioPkt.getAdditionalStatus());
            return;
        }

        var arr = ioPkt.getBuffer().array();
        int wordLength = arr.length / 4;
        if (arr.length % 4 != 0) {
            wordLength++;
        }

        cw.getBuffer().unpackQuarterWords(arr, false);

        channelProgram.setWordsTransferred(wordLength);
        channelProgram.setIoStatus(ioPkt.getStatus(), ioPkt.getAdditionalStatus());
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

        var bytes = new byte[cw.getBuffer().getSize()];
        cw.getBuffer().pack(bytes);
        var totalWordCount = cw.getTransferCount();
        var ioPkt = new SymbiontIoPacket();
        ioPkt.setBuffer(ByteBuffer.wrap(bytes))
             .setFunction(IoFunction.Write);
        device.startIo(ioPkt);
        if (ioPkt.getStatus() != IoStatus.Complete) {
            channelProgram.setIoStatus(ioPkt.getStatus(), ioPkt.getAdditionalStatus());
            return;
        }

        channelProgram.setWordsTransferred(totalWordCount);
        channelProgram.setIoStatus(ioPkt.getStatus(), ioPkt.getAdditionalStatus());
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SymbiontChannel;
    }
}
