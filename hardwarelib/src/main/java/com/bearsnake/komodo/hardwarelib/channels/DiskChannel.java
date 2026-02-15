/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.channels;

import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.devices.DiskDevice;
import com.bearsnake.komodo.hardwarelib.devices.DiskIoPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    public static final int DEFAULT_BUFFER_SIZE = 8192; // in bytes
    public static final Logger LOGGER = LogManager.getLogger(DiskChannel.class);

    private final LinkedBlockingQueue<DiskIoPacket> _freePackets = new LinkedBlockingQueue<>();

    public DiskChannel(final String nodeName) {
        super(nodeName);

        for (int i = 0; i < MAX_CONCURRENT_IOS; i++) {
            var ioPkt = new DiskIoPacket();
            ioPkt.setBuffer(ByteBuffer.allocate(DEFAULT_BUFFER_SIZE));
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
        LOGGER.trace("{}:routeIO {}", getNodeName(), channelPacket.toString());

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
                            LOGGER.info("{}:routeIO interrupted", getNodeName());
                        }
                    }
                }
            }
        }

        LOGGER.trace("{}:routeIO done:{}", getNodeName(), channelPacket.toString());
    }

    private void processRead(final DiskDevice device,
                             final ChannelIoPacket channelPacket,
                             final DiskIoPacket ioPacket) {
        var blockExtent = BlockExtent.determine(device, channelPacket);
        if (blockExtent == null) {
            return;
        }

        if (ioPacket.getBuffer().limit() < blockExtent._byteCount) {
            ioPacket.setBuffer(ByteBuffer.allocate((int)blockExtent._byteCount));
        }
        ioPacket.setBlockId(blockExtent._blockId)
                .setBlockCount(blockExtent._blockCount)
                .setFunction(IoFunction.Read);
        device.performIo(ioPacket);
        if (ioPacket.getStatus() == IoStatus.Successful) {
            var byteSlop = (blockExtent._preSlop / 28) * 128;
            byteSlop += (blockExtent._preSlop % 28) * 9 / 2;

            channelPacket.getBuffer().unpack(ioPacket.getBuffer().array(), byteSlop, (int)blockExtent._byteCount, true);
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
        var blockExtent = BlockExtent.determine(device, channelPacket);
        if (blockExtent == null) {
            return;
        }

        if (ioPacket.getBuffer().limit() < blockExtent._byteCount) {
            ioPacket.setBuffer(ByteBuffer.allocate((int)blockExtent._byteCount));
        }

        ioPacket.setBlockId(blockExtent._blockId)
                .setBlockCount(blockExtent._blockCount)
                .setFunction(IoFunction.Read);

        // Do we need to do read-before-write?
        if (blockExtent._preSlop > 0 || blockExtent._postSlop > 0) {
            ioPacket.setFunction(IoFunction.Read);
            device.performIo(ioPacket);
            if (ioPacket.getStatus() != IoStatus.Successful) {
                channelPacket.setIoStatus(ioPacket.getStatus())
                             .setAdditionalStatus(ioPacket.getAdditionalStatus());
                return;
            }
        }

        var byteSlop = (blockExtent._preSlop / 28) * 128;
        byteSlop += (blockExtent._preSlop % 28) * 9 / 2;

        channelPacket.getBuffer().pack(ioPacket.getBuffer().array(), byteSlop, true);
        channelPacket.setActualWordCount(channelPacket.getBuffer().getSize());
        ioPacket.setFunction(IoFunction.Write);
        device.performIo(ioPacket);
        if (ioPacket.getStatus() == IoStatus.Successful) {
            channelPacket.setActualWordCount(channelPacket.getBuffer().getSize());
        }

        channelPacket.setIoStatus(ioPacket.getStatus())
                     .setAdditionalStatus(ioPacket.getAdditionalStatus());
    }

    private static class BlockExtent {

        long _blockId;
        long _blockCount;
        int _bytesPerBlock;
        long _byteCount;
        int _preSlop; // words required to align backward to start of first block
        int _postSlop; // words required to align forward to end of last block

        static BlockExtent determine(final DiskDevice device,
                                     final ChannelIoPacket channelPacket) {
            var ioLength = channelPacket.getBuffer().getSize();
            if (ioLength % 2 > 0) {
                channelPacket.setIoStatus(IoStatus.InvalidBufferSize);
                return null;
            }

            if (channelPacket.getDeviceWordAddress() % 2 > 0) {
                channelPacket.setIoStatus(IoStatus.InvalidAddress);
                return null;
            }

            var blockExtent = new BlockExtent();

            var diskInfo = device.getInfo();
            blockExtent._bytesPerBlock = diskInfo.getBlockSize();
            var sectorsPerBlock = blockExtent._bytesPerBlock / 128; // a sector always fits in 128 bytes (2 bytes at the end are unused)
            var wordsPerBlock = sectorsPerBlock * 28;

            blockExtent._blockId = channelPacket.getDeviceWordAddress() / wordsPerBlock;
            blockExtent._preSlop = (int)(channelPacket.getDeviceWordAddress() % wordsPerBlock);
            var mod = (ioLength + blockExtent._preSlop) % wordsPerBlock;
            blockExtent._postSlop = mod > 0 ? wordsPerBlock - mod : 0;
            blockExtent._blockCount = (blockExtent._preSlop + ioLength + blockExtent._postSlop) / wordsPerBlock;
            blockExtent._byteCount = blockExtent._blockCount * blockExtent._bytesPerBlock;

            return blockExtent;
        }

        public void dump() {
            System.out.printf("BlockExtent bytes/block: %d\n", _bytesPerBlock);
            System.out.printf("            blockId:     %d\n", _blockId);
            System.out.printf("            blockCount:  %d\n", _blockCount);
            System.out.printf("            bytesCount:  %d\n", _byteCount);
            System.out.printf("            preSlop:     %d\n", _preSlop);
            System.out.printf("            postSlop:    %d\n", _postSlop);
        }
    }
}
