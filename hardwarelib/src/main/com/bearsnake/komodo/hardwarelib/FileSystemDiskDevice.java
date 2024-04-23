/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.logger.LogManager;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * A virtual disk which stores data in the host filesystem.
 * We assume blocks of 4k, so all block ids refer to 4k blocks.
 * Reads will get a newly-allocated buffer in the packet on successful return.
 * size considering our fixed block size, and the number of blocks requested.
 * Writes must provide a properly-sized buffer accounting for the fixed block size and number of blocks requested.
 */
public class FileSystemDiskDevice extends DiskDevice {

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
        var mi = new IoPacket.MountInfo(fileName, writeProtected);
        var pkt = new DiskIoPacket().setMountInfo(mi);
        doMount(pkt);
        if (pkt.getStatus() == IoStatus.Complete) {
            setIsReady(true);
        }
    }

    @Override
    public final DeviceModel getDeviceModel() {
        return DeviceModel.FileSystemDisk;
    }

    @Override
    public synchronized void startIo(final IoPacket packet) {
        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo(%s)", packet.toString());
        }

        if (packet instanceof DiskIoPacket diskPacket) {
            packet.setStatus(IoStatus.InProgress);
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
            LogManager.logTrace(_nodeName, "startIo status=%s", packet.getStatus());
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
        var info = getInfo();
        packet.getBuffer().clear();
        info.serialize(packet.getBuffer());
        packet.setStatus(IoStatus.Complete);
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
            System.out.printf("path:%s\n", path);
            if (packet.getMountInfo().getWriteProtected()) {
                _channel = FileChannel.open(path, CREATE, READ);
            } else {
                _channel = FileChannel.open(path, CREATE, READ, WRITE);
            }
        } catch (IOException ex) {
            LogManager.logError(_nodeName, "Error opening file:%s", ex);
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        _writeProtected = packet.getMountInfo().getWriteProtected();
        packet.setStatus(IoStatus.Complete);
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
            var bytes = _channel.read(buffer, packet.getBlockId() * BLOCK_SIZE);
            if (bytes != transferSize) {
                LogManager.logError(_nodeName, "Error - wanted %d bytes, got %d", transferSize, bytes);
                packet.setStatus(IoStatus.SystemError);
                return;
            }
        } catch (IOException ex) {
            LogManager.logError(_nodeName, "Error read from file:%s", ex);
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setBuffer(buffer);
        packet.setStatus(IoStatus.Complete);
    }

    private void doReset(final DiskIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        packet.setStatus(IoStatus.Complete);
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
            LogManager.logError(_nodeName, "Error closing file:%s", ex);
        }

        _channel = null;
        setIsReady(false);
        packet.setStatus(IoStatus.Complete);
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
                packet.setStatus(IoStatus.SystemError);
                return;
            }
        } catch (IOException ex) {
            LogManager.logError(_nodeName, "Error read from file:%s", ex);
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }
}
