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
 * A virtual tape device which reads and writes native file-system files as if they were tape volumes.
 * We support multiple versions/layouts of tape blocks.
 * Writes must provide a buffer containing the exact block of data to be written.
 */
public class FileSystemTapeDevice extends TapeDevice {

    private boolean _canRead = false;
    private FileChannel _channel;
    private boolean _writeProtected = false;
    private FSTapeTranslator _translator;

    public FileSystemTapeDevice(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final DeviceModel getDeviceModel() {
        return DeviceModel.FileSystemTape;
    }

    @Override
    public void startIo(final IoPacket packet) {
        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo(%s)", packet.toString());
        }

        if (packet instanceof TapeIoPacket tapePacket) {
            packet.setStatus(IoStatus.InProgress);
            switch (packet.getFunction()) {
            case GetInfo:
                doGetInfo(tapePacket);
            case Mount:
                doMount(tapePacket);
            case MoveBackward:
                doMoveBackward(tapePacket);
            case MoveForward:
                doMoveForward(tapePacket);
            case Read:
                doRead(tapePacket);
            case ReadBackward:
                doReadBackward(tapePacket);
            case Reset:
                doReset(tapePacket);
            case Rewind:
                doRewind(tapePacket);
            case RewindAndUnload:
                doRewindAndUnload(tapePacket);
            case Unmount:
                doUnmount(tapePacket);
            case Write:
                doWrite(tapePacket);
            case WriteTapeMark:
                doWriteTapeMark(tapePacket);
            default:
                packet.setStatus(IoStatus.InvalidFunction);
            }
        } else {
            packet.setStatus(IoStatus.InvalidPacket);
        }

        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo status=%s", packet.getStatus());
        }
    }

    @Override
    public synchronized String toString() {
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
    public TapeInfo getInfo() {
        boolean isMounted = _channel != null;
        return new TapeInfo(isMounted, isReady(), _writeProtected);
    }

    public boolean isMounted() { return _channel != null; }
    public boolean isWriteProtected() { return _writeProtected; }

    private synchronized void doGetInfo(final TapeIoPacket packet) {
        var info = getInfo();
        packet.getBuffer().reset();
        info.serialize(packet.getBuffer());
        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doMount(final TapeIoPacket packet) {
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
        _translator = new FSNativeTapeTranslator(_channel);
        _canRead = true;
        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doMoveBackward(final TapeIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (!_canRead) {
            packet.setStatus(IoStatus.ReadNotAllowed);
            return;
        }

        try {
            _translator.readBackward();
        } catch (FSTapeTranslator.DataException e) {
            packet.setStatus(IoStatus.DataException);
            throw new RuntimeException(e);
        } catch (FSTapeTranslator.EndOfTapeException ex) {
            packet.setStatus(IoStatus.EndOfTape);
            return;
        } catch (FSTapeTranslator.TapeMarkException ex) {
            packet.setStatus(IoStatus.EndOfFile);
            return;
        } catch (IOException ex) {
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doMoveForward(final TapeIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (!_canRead) {
            packet.setStatus(IoStatus.ReadNotAllowed);
            return;
        }

        try {
            _translator.readBackward();
        } catch (FSTapeTranslator.DataException e) {
            packet.setStatus(IoStatus.DataException);
            throw new RuntimeException(e);
        } catch (FSTapeTranslator.EndOfTapeException ex) {
            packet.setStatus(IoStatus.EndOfTape);
            return;
        } catch (FSTapeTranslator.TapeMarkException ex) {
            packet.setStatus(IoStatus.EndOfFile);
            return;
        } catch (IOException ex) {
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doRead(final TapeIoPacket packet) {
        if (packet._buffer == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (!_canRead) {
            packet.setStatus(IoStatus.ReadNotAllowed);
            return;
        }

        try {
            packet.setBuffer(_translator.read());
            packet.setBytesTransferred(packet.getBuffer().capacity());
        } catch (FSTapeTranslator.DataException ex) {
            packet.setStatus(IoStatus.DataException);
            return;
        } catch (FSTapeTranslator.TapeMarkException ex) {
            packet.setStatus(IoStatus.EndOfFile);
            return;
        } catch (IOException ex) {
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doReadBackward(final TapeIoPacket packet) {
        if (packet._buffer == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (!_canRead) {
            packet.setStatus(IoStatus.ReadNotAllowed);
            return;
        }

        try {
            packet.setBuffer(_translator.readBackward());
            packet.setBytesTransferred(packet.getBuffer().capacity());
        } catch (FSTapeTranslator.DataException e) {
            packet.setStatus(IoStatus.DataException);
            throw new RuntimeException(e);
        } catch (FSTapeTranslator.EndOfTapeException ex) {
            packet.setStatus(IoStatus.EndOfTape);
            return;
        } catch (FSTapeTranslator.TapeMarkException ex) {
            packet.setStatus(IoStatus.EndOfFile);
            return;
        } catch (IOException ex) {
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doReset(final TapeIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
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

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doRewind(final TapeIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        try {
            _channel.position(0);
        } catch (IOException ex) {
            packet.setStatus(IoStatus.Complete);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doRewindAndUnload(final TapeIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        try {
            _translator = null;
            _channel.close();
        } catch (IOException ex) {
            LogManager.logError(_nodeName, "Error closing file:%s", ex);
        }

        _channel = null;
        setIsReady(false);
        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doUnmount(final TapeIoPacket packet) {
        if (_channel == null) {
            packet.setStatus(IoStatus.MediaNotMounted);
            return;
        }

        try {
            _translator = null;
            _channel.close();
        } catch (IOException ex) {
            LogManager.logError(_nodeName, "Error closing file:%s", ex);
        }

        _channel = null;
        setIsReady(false);
        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doWrite(final TapeIoPacket packet) {
        if (packet._buffer == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_writeProtected) {
            packet.setStatus(IoStatus.WriteProtected);
            return;
        }

        try {
            int bytes = _translator.write(packet._buffer);
            _canRead = false;
            packet.setBytesTransferred(bytes);
        } catch (IOException ex) {
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private synchronized void doWriteTapeMark(final TapeIoPacket packet) {
        if (packet._buffer == null) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_writeProtected) {
            packet.setStatus(IoStatus.WriteProtected);
            return;
        }

        try {
            int bytes = _translator.writeTapeMark();
            _canRead = false;
            packet.setBytesTransferred(bytes);
        } catch (IOException ex) {
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }
}
