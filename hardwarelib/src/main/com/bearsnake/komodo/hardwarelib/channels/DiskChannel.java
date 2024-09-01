/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.channels;

import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.devices.DiskDevice;
import com.bearsnake.komodo.hardwarelib.devices.DiskIoPacket;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.logger.LogManager;

import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Channel specifically for byte-oriented disk devices
 * We convert words to bytes by packing 2 words into 9 bytes, and unpacking the other direction.
 * All data is written on the basis of 28-word sectors <-> 128 word portions,
 * so if the block size is 512 bytes, this equates to 4 28-word sectors (with 2 bytes of slop at the end of each sector).
 * ...
 * Channel-oriented IO is directed toward a particular disk unit.
 * Read/Write operations are specified with a starting device-relative word address and word count, neither of which
 * need to be aligned on block (or track) boundaries... but it is more efficient if they are on block boundaries.
 */
public class DiskChannel extends Channel {

    public static final int MAX_CONCURRENT_IOS = 8;
    public static final int MAX_TRANSFER_COUNT = 1792; // in words

    private final LinkedBlockingQueue<DiskIoPacket> _freePackets = new LinkedBlockingQueue<>();

    public DiskChannel(final String nodeName) {
        super(nodeName);

        var bufferSize = (MAX_TRANSFER_COUNT / 28) * 128;
        for (int i = 0; i < MAX_CONCURRENT_IOS; i++) {
            var ioPkt = new DiskIoPacket();
            ioPkt.setBuffer(ByteBuffer.allocate(bufferSize));
            _freePackets.add(ioPkt);
        }
    }

    @Override
    public boolean canAttach(
        final Device device
    ) {
        return device instanceof DiskDevice;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.DiskChannel;
    }

    @Override
    public void routeIo(final ChannelIoPacket channelPacket) {
        LogManager.logTrace(getNodeName(), "routeIo %s", channelPacket);

        channelPacket.setIoStatus(IoStatus.NotStarted);
        channelPacket.setAdditionalStatus(null);

        var nodeId = channelPacket.getNodeIdentifier();
        if (nodeId == getNodeIdentifier()) {
            // There is currently no IO function which we support as a Channel...
            channelPacket.setIoStatus(IoStatus.InvalidFunction);
        } else {
            if (!_devices.containsKey(nodeId)) {
                channelPacket.setIoStatus(IoStatus.DeviceIsNotAttached);
            } else {
                if (channelPacket.getFormat() != TransferFormat.Packed) {
                    channelPacket.setIoStatus(IoStatus.InvalidTransferFormat);
                } else {
                    boolean retry = true;
                    while (retry) {
                        try {
                            var ioPacket = _freePackets.take();
                            var device = (DiskDevice) _devices.get(nodeId);
                            switch (channelPacket.getIoFunction()) {
                                case Read -> processRead(device, channelPacket, ioPacket);
                                case GetInfo, Mount, Reset, Unmount -> processUtility(device, channelPacket, ioPacket);
                                case Write -> processWrite(device, channelPacket, ioPacket);
                                default -> channelPacket.setIoStatus(IoStatus.InvalidFunction);
                            }
                            _freePackets.add(ioPacket);
                            retry = false;
                        } catch (InterruptedException ex) {
                            LogManager.logCatching(getNodeName(), ex);
                        }
                    }
                }
            }
        }

        LogManager.logTrace(getNodeName(), "routeIo done:%s", channelPacket);
    }

    private void processRead(final DiskDevice device,
                             final ChannelIoPacket channelPacket,
                             final DiskIoPacket ioPacket) {
        var geometry = IoGeometry.determine(device, channelPacket);
        if (geometry._ioStatus != IoStatus.Successful) {
            channelPacket.setIoStatus(geometry._ioStatus);
            return;
        }

        ioPacket.setBlockId(geometry._blockId)
                .setBlockCount(geometry._blockCount)
                .setFunction(IoFunction.Read);
        device.performIo(ioPacket);

        if (ioPacket.getStatus() == IoStatus.Successful) {
            channelPacket.getBuffer().unpack(ioPacket.getBuffer().array(),
                                             geometry._leadingOffsetInBytes,
                                             geometry._requestedByteCount);
            channelPacket.setActualWordCount(channelPacket.getBuffer().getSize());
        }

        channelPacket.setIoStatus(IoStatus.Successful)
                     .setAdditionalStatus(ioPacket.getAdditionalStatus());
    }

    private void processUtility(final DiskDevice device,
                                final ChannelIoPacket channelPacket,
                                final IoPacket ioPacket) {

        ioPacket.setFunction(channelPacket.getIoFunction());
        if (channelPacket.getIoFunction() == IoFunction.Mount) {
            ioPacket.setMountInfo(channelPacket.getMountInfo());
        }

        device.performIo(ioPacket);
        if (channelPacket.getIoFunction() == IoFunction.GetInfo && ioPacket.getStatus() == IoStatus.Successful) {
            channelPacket.setDeviceInfo(ioPacket.getDeviceInfo());
        }

        channelPacket.setIoStatus(ioPacket.getStatus()).setAdditionalStatus(ioPacket.getAdditionalStatus());
    }

    private void processWrite(final DiskDevice device,
                              final ChannelIoPacket channelPacket,
                              final DiskIoPacket ioPacket) {
        var geometry = IoGeometry.determine(device, channelPacket);
        if (geometry._ioStatus != IoStatus.Successful) {
            channelPacket.setIoStatus(geometry._ioStatus);
            return;
        }

        ioPacket.setBlockId(geometry._blockId)
                .setBlockCount(geometry._blockCount);

        // Do we need to do read-before-write?
        if (channelPacket.getBuffer().getSize() != geometry._alignedWordCount) {
            ioPacket.setFunction(IoFunction.Read);
            device.performIo(ioPacket);
            if (ioPacket.getStatus() != IoStatus.Successful) {
                channelPacket.setIoStatus(ioPacket.getStatus())
                             .setAdditionalStatus(ioPacket.getAdditionalStatus());
                return;
            }
        }

        channelPacket.getBuffer().pack(ioPacket.getBuffer().array(),
                                       geometry._leadingOffsetInBytes);
        ioPacket.setFunction(IoFunction.Write);
        device.performIo(ioPacket);
        if (ioPacket.getStatus() == IoStatus.Successful) {
            channelPacket.setActualWordCount(channelPacket.getBuffer().getSize());
        }

        channelPacket.setIoStatus(ioPacket.getStatus())
                     .setAdditionalStatus(ioPacket.getAdditionalStatus());
    }

    private static class IoGeometry {

        IoStatus _ioStatus;
        int      _bytesPerBlock;
        int      _wordsPerBlock;
        int      _leadingOffsetInWords;
        int      _leadingOffsetInBytes;
        long     _requestedWordAddress;
        int      _requestedByteCount;
        long     _alignedWordAddress;
        int      _alignedWordCount;
        long     _blockId;
        int      _blockCount;

        static IoGeometry determine(final DiskDevice device,
                                    final ChannelIoPacket channelPacket) {
            var geometry = new IoGeometry();

            var diskInfo = device.getInfo();
            geometry._ioStatus = IoStatus.Successful;

            var buffer = channelPacket.getBuffer();
            if (buffer == null) {
                geometry._ioStatus = IoStatus.BufferIsNull;
                return geometry;
            }

            if ((buffer.getSize() & 01) != 0) {
                geometry._ioStatus = IoStatus.InvalidBufferSize;
                return geometry;
            }

            if ((channelPacket.getDeviceWordAddress() & 01) != 0) {
                geometry._ioStatus = IoStatus.InvalidAddress;
                return geometry;
            }

            geometry._bytesPerBlock = diskInfo.getBlockSize();
            geometry._wordsPerBlock = (geometry._bytesPerBlock / 128) * 28;
            if (buffer.getSize() % geometry._wordsPerBlock != 0) {
                geometry._ioStatus = IoStatus.InvalidBufferSize;
                return geometry;
            }

            geometry._requestedWordAddress = channelPacket.getDeviceWordAddress();
            if ((geometry._requestedWordAddress & 01) != 0) {
                geometry._ioStatus = IoStatus.InvalidAddress;
                return geometry;
            }

            geometry._leadingOffsetInWords = (int)(channelPacket.getDeviceWordAddress() % geometry._wordsPerBlock);
            geometry._leadingOffsetInBytes = (geometry._leadingOffsetInWords >> 1) * 9;
            geometry._requestedByteCount = (buffer.getSize() >> 1) * 9;
            geometry._alignedWordCount = geometry._leadingOffsetInWords + buffer.getSize();
            var mod = geometry._alignedWordCount % geometry._wordsPerBlock;
            if (mod > 0) {
                geometry._alignedWordCount += geometry._wordsPerBlock - mod;
            }

            geometry._alignedWordAddress = geometry._requestedWordAddress - geometry._leadingOffsetInWords;
            geometry._blockId = geometry._alignedWordAddress / geometry._wordsPerBlock;
            geometry._blockCount = geometry._alignedWordCount / geometry._wordsPerBlock;

            return geometry;
        }
    }
}
