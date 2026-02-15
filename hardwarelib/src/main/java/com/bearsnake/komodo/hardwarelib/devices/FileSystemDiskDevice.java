/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;

import static java.nio.file.StandardOpenOption.*;

/**
 * A virtual disk which stores data in the host filesystem.
 * We assume blocks of 4k, so all block ids refer to 4k blocks.
 * Reads will get a newly-allocated buffer in the packet on successful return.
 * size considering our fixed block size, and the number of blocks requested.
 * Writes must provide a properly-sized buffer accounting for the fixed block size and number of blocks requested.
 */
public class FileSystemDiskDevice extends DiskDevice {

    private static final Logger LOGGER = LogManager.getLogger(FileSystemDiskDevice.class);

    private static final int BLOCK_SIZE = 4096;
    private static final int MAX_BLOCK_COUNT = 262143; // could be Integer.MAX_VALUE / 4096;
    private FileChannel _channel;
    private boolean _writeProtected = false;

    // Normal constructor
    public FileSystemDiskDevice(final String nodeName) {
        super(nodeName);
    }

    // Auto-mount constructor
    public FileSystemDiskDevice(final String nodeName,
                                final String fileName,
                                final boolean writeProtected) {
        super(nodeName);
        var mi = new MountInfo(fileName, writeProtected);
        var pkt = new DiskIoPacket();
        pkt.setMountInfo(mi);
        doMount(pkt);
        if (pkt.getStatus() == IoStatus.Successful) {
            setIsReady(true);
        }
    }

    @Override
    public final DeviceModel getDeviceModel() {
        return DeviceModel.FileSystemDisk;
    }

    @Override
    public final void probe() {}

    @Override
    public synchronized void performIo(final IoPacket packet) {
        if (_logIos) {
            LOGGER.trace("{}:performIo enter({})", _nodeName, packet.toString());
        }

        if (packet instanceof DiskIoPacket diskPacket) {
            switch (packet.getFunction()) {
                case GetInfo -> doGetInfo(diskPacket);
                case Mount -> doMount(diskPacket);
                case Read -> doRead(diskPacket);
                case Reset -> doReset(diskPacket);
                case Unmount -> doUnmount(diskPacket);
                case Write -> doWrite(diskPacket);
                default -> packet.setStatus(IoStatus.InvalidFunction);
            }
        } else {
            packet.setStatus(IoStatus.InvalidPacket);
        }

        if (_logIos) {
            LOGGER.trace("{}:performIo exit({})", _nodeName, packet.toString());
        }
    }

    @Override
    public String toString() {
        return String.format("%s %s:%s:%s mnt:%s rdy:%s wp:%s",
                             getNodeName(),
                             getNodeCategory(),
                             getDeviceType(),
                             getDeviceModel(),
                             isMounted(),
                             isReady(),
                             isWriteProtected());
    }

    @Override
    public DiskInfo getInfo() {
        boolean isMounted = _channel != null;
        int blockCount = 0;
        try {
            if (isMounted) {
                blockCount = (int)(_channel.size() / BLOCK_SIZE);
            }
        } catch (IOException ex) {
            // do nothing
        }

        return new DiskInfo(BLOCK_SIZE, blockCount, MAX_BLOCK_COUNT, isMounted, isReady(), _writeProtected);
    }

    public boolean isMounted() { return _channel != null; }
    public boolean isWriteProtected() { return _writeProtected; }

    private void doGetInfo(final DiskIoPacket packet) {
        packet.setDeviceInfo(getInfo());
        packet.setStatus(IoStatus.Successful);
    }

    private void doMount(final DiskIoPacket packet) {
        if (packet.getMountInfo() == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (_channel != null) {
            packet.setStatus(IoStatus.MediaAlreadyMounted);
            return;
        }

        try {
            var path = FileSystems.getDefault().getPath(packet.getMountInfo().getFileName());
            if (packet.getMountInfo().getWriteProtected()) {
                _channel = FileChannel.open(path, CREATE, READ);
            } else {
                _channel = FileChannel.open(path, CREATE, READ, WRITE);
            }
        } catch (IOException ex) {
            LOGGER.error("{}:Error opening file:{}", _nodeName, ex);
            packet.setStatus(IoStatus.SystemError).setAdditionalStatus(ex.getMessage());
            return;
        }

        _writeProtected = packet.getMountInfo().getWriteProtected();
        packet.setStatus(IoStatus.Successful);
    }

    private void doRead(final DiskIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        long blockCount = packet.getBlockCount();
        long blockId = packet.getBlockId();

        if ((blockId < 0) || (blockId >= MAX_BLOCK_COUNT)) {
            packet.setStatus(IoStatus.InvalidBlockId);
            return;
        }

        if ((blockCount < 0) || (blockId + blockCount > MAX_BLOCK_COUNT)) {
            packet.setStatus(IoStatus.InvalidBlockCount);
            return;
        }

        int transferSize = (int)(blockCount * BLOCK_SIZE);
        var buffer = ByteBuffer.allocate(transferSize);

        try {
            // Is any part of the request in low-level unallocated space?
            // Normal disk devices don't have unallocated space, just unwritten space.
            // However, we only grow the underlying file as necessary, so we have to content with this.
            long bytes;
            long currentSize = _channel.size();
            long requiredSize = (packet.getBlockId() * BLOCK_SIZE) + transferSize;
            if (requiredSize > currentSize) {
                var bb = ByteBuffer.allocate(BLOCK_SIZE);
                bytes = _channel.write(bb, requiredSize - BLOCK_SIZE);
            }

            bytes = _channel.read(buffer, packet.getBlockId() * BLOCK_SIZE);
            if (bytes != transferSize) {
                LOGGER.info("{}:Note - we wanted {} bytes, but we got {}", _nodeName, transferSize, bytes);
                packet.setStatus(IoStatus.SystemError).setAdditionalStatus("Data underrun");
                return;
            }
        } catch (IOException ex) {
            LOGGER.error("{}:Error reading from file:{}", _nodeName, ex);
            packet.setStatus(IoStatus.SystemError).setAdditionalStatus(ex.getMessage());
            return;
        }

        packet.setBuffer(buffer);
        packet.setStatus(IoStatus.Successful);
    }

    private void doReset(final DiskIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        packet.setStatus(IoStatus.Successful);
    }

    private void doUnmount(final DiskIoPacket packet) {
        if (packet.getMountInfo() == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (_channel == null) {
            packet.setStatus(IoStatus.MediaNotMounted);
            return;
        }

        try {
            _channel.close();
        } catch (IOException ex) {
            LOGGER.error("{}:Error closing file:{}", _nodeName, ex);
        }

        _channel = null;
        setIsReady(false);
        packet.setStatus(IoStatus.Successful);
    }

    private void doWrite(final DiskIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_writeProtected) {
            packet.setStatus(IoStatus.WriteProtected);
            return;
        }

        long blockCount = packet.getBlockCount();
        long blockId = packet.getBlockId();

        if ((blockId < 0) || (blockId >= MAX_BLOCK_COUNT)) {
            packet.setStatus(IoStatus.InvalidBlockId);
            return;
        }

        if ((blockCount < 0) || (blockId + blockCount > MAX_BLOCK_COUNT)) {
            packet.setStatus(IoStatus.InvalidBlockCount);
            return;
        }

        ByteBuffer buffer = packet.getBuffer();
        int transferSize = (int)(blockCount * BLOCK_SIZE);
        if (packet.getBuffer().capacity() != transferSize) {
            packet.setStatus(IoStatus.InvalidBufferSize);
            return;
        }
        buffer.rewind();

        try {
            var bytes = _channel.write(buffer, packet.getBlockId() * BLOCK_SIZE);
            if (bytes != transferSize) {
                packet.setStatus(IoStatus.SystemError).setAdditionalStatus("Transfer length not as expected");
                return;
            }
        } catch (IOException ex) {
            LOGGER.error("{}:Error writing to file:{}", _nodeName, ex);
            packet.setStatus(IoStatus.SystemError).setAdditionalStatus(ex.getMessage());
            return;
        }

        packet.setStatus(IoStatus.Successful);
    }
}
