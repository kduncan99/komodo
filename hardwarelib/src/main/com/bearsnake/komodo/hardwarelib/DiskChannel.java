/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import java.nio.ByteBuffer;

public class DiskChannel extends Channel {

    public DiskChannel(final String nodeName) {
        super(nodeName);
    }

    @Override
    public boolean canAttach(
        final Device device
    ) {
        return device instanceof DiskDevice;
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
        if (channelProgram._controlWords.isEmpty()) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        for (var cw : channelProgram._controlWords) {
            if (cw.getBuffer() == null) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getTransferCount() <= 0) || ((cw.getTransferCount() & 0x01) != 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getBufferOffset() < 0) || (cw.getBufferOffset() >= cw.getBuffer().getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getDirection() == ChannelProgram.Direction.Increment)
                && (cw.getBufferOffset() + cw.getTransferCount() > cw.getBuffer().getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            } else if ((cw.getDirection() == ChannelProgram.Direction.Decrement)
                && (cw.getBufferOffset() + 1 - cw.getTransferCount() < 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }
        }

        int totalWordCount = 0;
        for (var cw : channelProgram._controlWords) {
            if ((cw.getTransferCount() <= 0) || ((cw.getTransferCount() & 0x01) != 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getBufferOffset() < 0) || (cw.getBufferOffset() >= cw.getBuffer().getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getDirection() == ChannelProgram.Direction.Increment)
                && (cw.getBufferOffset() + cw.getTransferCount() > cw.getBuffer().getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            } else if ((cw.getDirection() == ChannelProgram.Direction.Decrement)
                && (cw.getBufferOffset() + 1 - cw.getTransferCount() < 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            totalWordCount += cw.getTransferCount();
        }

        var diskInfo = ((DiskDevice)device).getInfo();
        var wordsPerBlock = (diskInfo.getBlockSize() / 128) * 28;
        var totalBlockCount = totalWordCount / wordsPerBlock;
        if (totalWordCount % wordsPerBlock > 0) {
            totalBlockCount++;
        }

        var ioPkt = new DiskIoPacket();
        ioPkt.setBlockId(channelProgram._blockId)
             .setBlockCount(totalBlockCount)
             .setFunction(IoFunction.Read);
        device.startIo(ioPkt);
        if (ioPkt.getStatus() != IoStatus.Complete) {
            channelProgram.setIoStatus(ioPkt.getStatus());
            return;
        }

        var iter = channelProgram._controlWords.iterator();
        var bx = 0;
        while (iter.hasNext()) {
            var cw = iter.next();
            var wx = cw.getBufferOffset();
            var increment = switch(cw.getDirection()) {
                case Increment -> 1;
                case Decrement -> -1;
                default -> 0;
            };

            var buffer = ioPkt.getBuffer();
            int remaining = cw.getTransferCount();
            while (remaining > 0) {
                long w0 = ((long)(buffer.get(bx++)) & 0xff) << 28
                    | ((long)(buffer.get(bx++)) & 0xff) << 20
                    | ((long)(buffer.get(bx++)) & 0xff) << 12
                    | ((long)(buffer.get(bx++)) & 0xff) << 4
                    | (long)(buffer.get(bx) >> 4) & 0x0F;
                long w1 = (((long)(buffer.get(bx++)) & 0x0F) << 32)
                    | ((long)(buffer.get(bx++)) & 0xff) << 24
                    | ((long)(buffer.get(bx++)) & 0xff) << 16
                    | ((long)(buffer.get(bx++)) & 0xff) << 8
                    | ((long)(buffer.get(bx++)) & 0xff);
                cw.getBuffer()._array[wx] = w0;
                wx += increment;
                cw.getBuffer()._array[wx] = w1;
                wx += increment;

                if (bx % 128 == 126) {
                    bx += 2;
                }
                remaining -= 2;
            }
        }

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
        if (channelProgram._controlWords.isEmpty()) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        int totalWordCount = 0;
        for (var cw : channelProgram._controlWords) {
            if (cw.getBuffer() == null) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getTransferCount() <= 0) || ((cw.getTransferCount() & 0x01) != 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getBufferOffset() < 0) || (cw.getBufferOffset() >= cw.getBuffer().getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw.getDirection() == ChannelProgram.Direction.Increment)
                && (cw.getBufferOffset() + cw.getTransferCount() > cw.getBuffer().getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            } else if ((cw.getDirection() == ChannelProgram.Direction.Decrement)
                && (cw.getBufferOffset() + 1 - cw.getTransferCount() < 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            totalWordCount += cw.getTransferCount();
        }

        var diskInfo = ((DiskDevice)device).getInfo();
        var wordsPerBlock = (diskInfo.getBlockSize() / 128) * 28;
        var totalBlockCount = totalWordCount / wordsPerBlock;

        int totalByteCount = totalBlockCount * diskInfo.getBlockSize();
        var buffer = ByteBuffer.allocate(totalByteCount);

        var ioPkt = new DiskIoPacket().setBuffer(buffer)
                                      .setBlockId(channelProgram._blockId)
                                      .setBlockCount(totalBlockCount);

        if (totalWordCount % wordsPerBlock > 0) {
            // IO is not a multiple of the block size - we need to do a read-before-write
            totalBlockCount++;
            ioPkt.setBlockCount(totalBlockCount).setFunction(IoFunction.Read);
            device.startIo(ioPkt);
            if (ioPkt.getStatus() != IoStatus.Complete) {
                channelProgram.setIoStatus(ioPkt.getStatus());
                return;
            }
        }

        // Now populate the buffer
        var iter = channelProgram._controlWords.iterator();
        var bx = 0;
        while (iter.hasNext()) {
            var cw = iter.next();
            var wx = cw.getBufferOffset();
            var increment = switch(cw.getDirection()) {
                case Increment -> 1;
                case Decrement -> -1;
                default -> 0;
            };

            int remaining = cw.getTransferCount();
            while (remaining > 0) {
                long w0 = cw.getBuffer()._array[wx];
                wx += increment;
                long w1 = cw.getBuffer()._array[wx];
                wx += increment;

                buffer.put(bx++, (byte)(w0 >> 28));
                buffer.put(bx++, (byte)(w0 >> 20));
                buffer.put(bx++, (byte)(w0 >> 12));
                buffer.put(bx++, (byte)(w0 >> 4));
                var b = (byte)((byte)(w0 << 4) | (byte)(w1 >> 32));
                buffer.put(bx++, b);
                buffer.put(bx++, (byte)(w1 >> 24));
                buffer.put(bx++, (byte)(w1 >> 16));
                buffer.put(bx++, (byte)(w1 >> 8));
                buffer.put(bx++, (byte)(w1));

                if (bx % 128 == 126) {
                    bx += 2;
                }
                remaining -= 2;
            }
        }

        // Finally we can do the write
        ioPkt.setFunction(IoFunction.Write);
        device.startIo(ioPkt);
        channelProgram.setIoStatus(ioPkt.getStatus());
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.DiskChannel;
    }
}
