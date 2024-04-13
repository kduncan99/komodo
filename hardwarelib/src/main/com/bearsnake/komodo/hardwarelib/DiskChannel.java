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
    public boolean canAttach(Device device) {
        return device instanceof DiskDevice;
    }

    @Override
    public void doRead(final ChannelProgram channelProgram,
                       final Device device) {
        if (channelProgram._controlWords.isEmpty()) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        for (var cw : channelProgram._controlWords) {
            if ((cw._transferCount <= 0) || ((cw._transferCount & 0x01) != 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw._bufferOffset < 0) || (cw._bufferOffset >= cw._buffer.getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw._direction == ChannelProgram.Direction.Increment)
                && (cw._bufferOffset + cw._transferCount > cw._buffer.getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            } else if ((cw._direction == ChannelProgram.Direction.Decrement)
                && (cw._bufferOffset + 1 - cw._transferCount < 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }
        }

        int totalWordCount = 0;
        for (var cw : channelProgram._controlWords) {
            if ((cw._transferCount <= 0) || ((cw._transferCount & 0x01) != 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw._bufferOffset < 0) || (cw._bufferOffset >= cw._buffer.getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw._direction == ChannelProgram.Direction.Increment)
                && (cw._bufferOffset + cw._transferCount > cw._buffer.getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            } else if ((cw._direction == ChannelProgram.Direction.Decrement)
                && (cw._bufferOffset + 1 - cw._transferCount < 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            totalWordCount += cw._transferCount;
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
            var wx = cw._bufferOffset;
            var increment = switch(cw._direction) {
                case Increment -> 1;
                case Decrement -> -1;
                default -> 0;
            };

            var buffer = ioPkt.getBuffer();
            int remaining = cw._transferCount;
            while (remaining > 0) {
                long w0 = (long)(buffer.get(bx++)) << 28
                    | (long)(buffer.get(bx++)) << 20
                    | (long)(buffer.get(bx++)) << 12
                    | (long)(buffer.get(bx++)) << 4
                    | (long)(buffer.get(bx) >> 4);
                long w1 = ((long)(buffer.get(bx++) & 0x0F) << 32)
                    | (long)(buffer.get(bx++)) << 24
                    | (long)(buffer.get(bx++)) << 16
                    | (long)(buffer.get(bx++)) << 8
                    | (long)(buffer.get(bx++));

                cw._buffer._array[wx] = w0;
                wx += increment;
                cw._buffer._array[wx] = w1;
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
    public void doWrite(final ChannelProgram channelProgram,
                        final Device device) {
        if (channelProgram._controlWords.isEmpty()) {
            channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
            return;
        }

        int totalWordCount = 0;
        for (var cw : channelProgram._controlWords) {
            if ((cw._transferCount <= 0) || ((cw._transferCount & 0x01) != 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw._bufferOffset < 0) || (cw._bufferOffset >= cw._buffer.getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            if ((cw._direction == ChannelProgram.Direction.Increment)
                && (cw._bufferOffset + cw._transferCount > cw._buffer.getSize())) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            } else if ((cw._direction == ChannelProgram.Direction.Decrement)
                && (cw._bufferOffset + 1 - cw._transferCount < 0)) {
                channelProgram.setIoStatus(IoStatus.InvalidChannelProgram);
                return;
            }

            totalWordCount += cw._transferCount;
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
            var wx = cw._bufferOffset;
            var increment = switch(cw._direction) {
                case Increment -> 1;
                case Decrement -> -1;
                default -> 0;
            };

            int remaining = cw._transferCount;
            while (remaining > 0) {
                long w0 = cw._buffer._array[wx];
                wx += increment;
                long w1 = cw._buffer._array[wx];
                wx += increment;

                buffer.put(bx++, (byte)(w0 >> 28));
                buffer.put(bx++, (byte)(w0 >> 20));
                buffer.put(bx++, (byte)(w0 >> 12));
                buffer.put(bx++, (byte)(w0 >> 4));
                var b = (byte)((byte)(w0 << 4) | (byte)(w1 >> 32));
                buffer.put(bx++, b);
                buffer.put(bx++, (byte)(w1 >> 32));
                buffer.put(bx++, (byte)(w1 >> 24));
                buffer.put(bx++, (byte)(w1 >> 16));
                buffer.put(bx++, (byte)(w1 >> 8));

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
