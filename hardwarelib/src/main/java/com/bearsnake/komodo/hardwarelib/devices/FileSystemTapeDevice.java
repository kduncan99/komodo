/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;

import static java.nio.file.StandardOpenOption.*;

/**
 * A virtual tape device which reads and writes native file-system files as if they were tape volumes.
 * We support multiple versions/layouts of tape blocks.
 * Writes must provide a buffer containing the exact block of data to be written.
 */
public class FileSystemTapeDevice extends TapeDevice {

    private static final Logger LOGGER = LogManager.getLogger(FileSystemTapeDevice.class);

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
    public final void probe() {}

    @Override
    public void performIo(final IoPacket packet) {
        if (_logIos) {
            LOGGER.trace("{}:performIo enter({})", _nodeName, packet.toString());
        }

        if (packet instanceof TapeIoPacket tapePacket) {
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
                case WriteEndOfFile:
                    doWriteTapeMark(tapePacket);
                default:
                    packet.setStatus(IoStatus.InvalidFunction);
            }
        } else {
            packet.setStatus(IoStatus.InvalidPacket);
        }

        if (_logIos) {
            LOGGER.trace("{}:performIo exit({})", _nodeName, packet.toString());
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
        packet.setDeviceInfo(getInfo());
        packet.setStatus(IoStatus.Successful);
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
        _translator = new FSNativeTapeTranslator(_channel);
        _canRead = true;
        packet.setStatus(IoStatus.Successful);
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

        packet.setStatus(IoStatus.Successful);
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

        packet.setStatus(IoStatus.Successful);
    }

    private synchronized void doRead(final TapeIoPacket packet) {
        if (packet.getBuffer() == null) {
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

        packet.setStatus(IoStatus.Successful);
    }

    private synchronized void doReadBackward(final TapeIoPacket packet) {
        if (packet.getBuffer() == null) {
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

        packet.setStatus(IoStatus.Successful);
    }

    private synchronized void doReset(final TapeIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
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

        packet.setStatus(IoStatus.Successful);
    }

    private synchronized void doRewind(final TapeIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        try {
            _channel.position(0);
        } catch (IOException ex) {
            packet.setStatus(IoStatus.Successful);
            return;
        }

        packet.setStatus(IoStatus.Successful);
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
            LOGGER.error("{}:Error closing file:{}", _nodeName, ex);
        }

        _channel = null;
        setIsReady(false);
        packet.setStatus(IoStatus.Successful);
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
            LOGGER.error("{}:Error closing file:{}", _nodeName, ex);
        }

        _channel = null;
        setIsReady(false);
        packet.setStatus(IoStatus.Successful);
    }

    private synchronized void doWrite(final TapeIoPacket packet) {
        if (packet.getBuffer() == null) {
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
            int bytes = _translator.write(packet.getBuffer());
            _canRead = false;
            packet.setBytesTransferred(bytes);
        } catch (IOException ex) {
            packet.setStatus(IoStatus.SystemError);
            return;
        }

        packet.setStatus(IoStatus.Successful);
    }

    private synchronized void doWriteTapeMark(final TapeIoPacket packet) {
        if (packet.getBuffer() == null) {
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

        packet.setStatus(IoStatus.Successful);
    }
}
