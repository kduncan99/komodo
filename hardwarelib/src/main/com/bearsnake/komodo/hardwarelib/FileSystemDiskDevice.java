/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.logger.LogManager;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * A virtual disk which stores data in the host filesystem.
 * We assume blocks of 4k, so all block ids refer to 4k blocks.
 */
public abstract class FileSystemDiskDevice extends Device {

    private static final int BLOCK_SIZE = 4096;

    private FileChannel _channel;
    private boolean _isReady = false;
    private boolean _writeProtected = false;

    public FileSystemDiskDevice(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final DeviceType getDeviceType() {
        return DeviceType.DiskDevice;
    }

    @Override
    public void startIo(final IoPacket packet) {
        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo(%s)", packet.toString());
        }

        if (packet instanceof DiskIoPacket diskPacket) {
            packet.setStatus(IoStatus.InProgress);
            switch (packet.getFunction()) {
            case GetInfo:
                doGetInfo(diskPacket);
            case Mount:
                doMount(diskPacket);
            case Read:
                doRead(diskPacket);
            case Reset:
                doReset(diskPacket);
            case Unmount:
                doUnmount(diskPacket);
            case Write:
                doWrite(diskPacket);
            default:
                packet.setStatus(IoStatus.InvalidFunction);
            }
        } else {
            packet.setStatus(IoStatus.InvalidPacket);
        }

        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo status=%s", packet.getStatus());
        }
        if (packet.getListener() != null) {
            packet.getListener().ioComplete(packet);
        }
    }

    private synchronized void doGetInfo(final DiskIoPacket packet) {
        boolean isMounted = _channel != null;
        int blockCount = 0;
        try {
            if (isMounted) {
                blockCount = (int)(_channel.size() / BLOCK_SIZE);
            }
        } catch (IOException ex) {
            // do nothing
        }

        var info = new DiskIoPacket.Info(BLOCK_SIZE, blockCount, isMounted, _isReady, _writeProtected);
        packet.getBuffer().reset();
        info.serialize(packet.getBuffer());
        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doMount(final DiskIoPacket packet) {
        if (packet.getMountInfo() == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (_channel == null) {
            packet.setStatus(IoStatus.MediaNotMounted);
            return;
        }

        try {
            var path = FileSystems.getDefault().getPath(packet.getMountInfo().getFileName());
            if (packet._mountInfo.getWriteProtected()) {
                _channel = FileChannel.open(path, CREATE, READ);
            } else {
                _channel = FileChannel.open(path, CREATE, READ, WRITE);
            }
        } catch (IOException ex) {
            LogManager.logError(_nodeName, "Error opening file:%s", ex);
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        _writeProtected = packet._mountInfo.getWriteProtected();
        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doRead(final DiskIoPacket packet) {
        if (packet._buffer == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (!_isReady) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (packet._buffer.capacity() != BLOCK_SIZE) {
            packet.setStatus(IoStatus.InvalidBufferSize);
            return;
        }

        try {
            var blocks = _channel.size() / BLOCK_SIZE;
            if ((packet._blockId < 0) || (packet._blockId >= blocks)) {
                packet.setStatus(IoStatus.InvalidBlockId);
                return;
            }

            var offset = packet._blockId * BLOCK_SIZE;
            var bytes = _channel.read(packet._buffer, offset);
            if (bytes != BLOCK_SIZE) {
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

    private synchronized void doReset(final DiskIoPacket packet) {
        if (!_isReady) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doUnmount(final DiskIoPacket packet) {
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
        _isReady = false;
        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doWrite(final DiskIoPacket packet) {
        if (packet._buffer == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (!_isReady) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_writeProtected) {
            packet.setStatus(IoStatus.WriteProtected);
            return;
        }

        if (packet._buffer.capacity() != BLOCK_SIZE) {
            packet.setStatus(IoStatus.InvalidBufferSize);
            return;
        }

        try {
            var blocks = _channel.size() / BLOCK_SIZE;
            if ((packet._blockId < 0) || (packet._blockId >= blocks)) {
                packet.setStatus(IoStatus.InvalidBlockId);
                return;
            }

            var offset = packet._blockId * BLOCK_SIZE;
            var bytes = _channel.read(packet._buffer, offset);
            if (bytes != BLOCK_SIZE) {
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
