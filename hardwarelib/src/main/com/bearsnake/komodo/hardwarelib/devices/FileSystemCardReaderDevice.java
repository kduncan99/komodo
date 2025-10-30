/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.logger.LogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.stream.Stream;

public class FileSystemCardReaderDevice extends SymbiontReaderDevice {

    private final String _fileSystemPath;
    private String _fileName = null;
    private BufferedReader _reader = null;

    public FileSystemCardReaderDevice(
        final String nodeName,
        final String fileSystemPath // path of directory we watch for input files
    ) {
        super(nodeName);
        _fileSystemPath = fileSystemPath.endsWith("/") ? fileSystemPath : fileSystemPath + '/';
    }

    @Override
    public final void close() {}

    @Override
    public final DeviceModel getDeviceModel() {
        return DeviceModel.FileSystemImageReader;
    }

    @Override
    public final boolean isReady() {
        return _reader != null;
    }

    @Override
    public void setIsReady(boolean flag) {
        if (isReady() && !flag) {
            dropAndClose();
        }
        super.setIsReady(flag);
    }

    /*
     * Auto control of ready flag.
     * If we *are* ready and there is not a file being processed,
     *  we search for an input and if it exists, it is opened, and we set ready to true.
     *  Otherwise, we set ready to false.
     * If we are *not* ready, there cannot be a file in process.
     * We search for an input file in the source path, and if such a file exists, it is opened, and we set ready to true.
     */
    @Override
    public void probe() {
        // TODO why is _reader always null when we reach this code?
        //   because no one calls us otherwise? (which is fine, I guess, but we need UT for this).
        if (isReady() && (_reader == null)) {
            setIsReady(openInputFile());
        } else if (!isReady()) {
            if (openInputFile()) {
                setIsReady(true);
            }
        }
    }

    @Override
    public synchronized void performIo(final IoPacket packet) {
        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo(%s)", packet.toString());
        }

        if (packet instanceof SymbiontIoPacket symPacket) {
            switch (packet.getFunction()) {
                case Read -> doRead(symPacket);
                case Reset -> doReset(symPacket);
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
        return String.format("%s %s:%s:%s rdy:%s",
                             getNodeName(),
                             getNodeCategory(),
                             getDeviceType(),
                             getDeviceModel(),
                             isReady());
    }

    private void doRead(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        try {
            var input = _reader.readLine();
            if (input == null) {
                packet.setStatus(IoStatus.EndOfFile);
                dropAndClose();
                return;
            }

            var mod = input.length() % 4;
            if (mod > 0) {
                input += "    ".substring(mod);
            }
            packet.setBuffer(ByteBuffer.wrap(input.getBytes()));
            packet.setStatus(IoStatus.Successful);
        } catch (IOException ex) {
            dropAndClose();
            packet.setStatus(IoStatus.SystemError).setAdditionalStatus(ex.getMessage());
        }
    }

    private void doReset(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        dropAndClose();
        packet.setStatus(IoStatus.Successful);
    }

    /**
     * If there is a file open, close and delete it.
     */
    private void dropAndClose() {
        if (_reader != null) {
            var f = new File(_fileName);
            try {
                LogManager.logInfo(_nodeName, "Deleting %s...", f.toPath());
                Files.deleteIfExists(f.toPath());
            } catch (IOException ex) {
                // Cannot delete the file, we have to set machine check flag
                LogManager.logCatching(_nodeName, ex);
                // TODO implement machine check flag in Node, which prevents all IO, sets not-ready,
                //   and can only be cleared by .... what? DN and UP?
                //   This is necessary to prevent looping on the same input file, since we cannot seem to delete it.
            }
            _fileName = null;

            try {
                _reader.close();
            } catch (IOException ex) {
                // nothing can be done
            }
            _reader = null;
        }
    }

    /**
     * Invoked when we are able to process input, and have none in progress.
     */
    private boolean openInputFile() {
        var f = new File(_fileSystemPath).listFiles();

        if (f != null) {
            var fileList = Stream.of(f)
                                 .filter(file -> !file.isDirectory())
                                 .map(File::getName)
                                 .toList();
            if (!fileList.isEmpty()) {
                // Any time a file appears in the directory, it automatically triggers ready.
                // Even if the exec thinks the device is DN or SM L'd, it can still be ready.
                _fileName = _fileSystemPath + fileList.getFirst();
                LogManager.logDebug(getNodeName(), "Found file %s", _fileName);
                try {
                    _reader = new BufferedReader(new FileReader(_fileName));
                    return true;
                } catch (IOException ex) {
                    _reader = null;
                    _fileName = null;
                }
            }
        }

        return false;
    }
}
