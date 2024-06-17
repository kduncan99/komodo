/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.logger.LogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.stream.Stream;

// TODO need to implement SymbiontChannel
// TODO need a symbiontManager (in kexec) with a runnable which occasionally polls for this going ready,
//   then does the needful, resulting in a READ$X<runid> file and a BL entry (persisted to GENF$)

public class FileSystemImageReaderDevice extends SymbiontDevice implements Runnable {

    private final String _fileSystemPath;
    private boolean _terminate = false;
    private String _fileName = null;
    private BufferedReader _reader = null;

    public FileSystemImageReaderDevice(
        final String nodeName,
        final String fileSystemPath // path of directory we watch for input files
    ) {
        super(nodeName);
        _fileSystemPath = fileSystemPath.endsWith("/") ? fileSystemPath : fileSystemPath + '/';
    }

    @Override
    public final DeviceModel getDeviceModel() {
        return DeviceModel.FileSystemImageReader;
    }

    @Override
    public final boolean isReady() {
        return _reader != null;
    }

    @Override
    public synchronized void startIo(final IoPacket packet) {
        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo(%s)", packet.toString());
        }

        if (packet instanceof DiskIoPacket diskPacket) {
            packet.setStatus(IoStatus.InProgress);
            switch (packet.getFunction()) {
                case Read -> doRead(diskPacket);
                case Reset -> doReset(diskPacket);
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

    private void doRead(final DiskIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        try {
            var input = _reader.readLine();
            if (input == null) {
                packet.setStatus(IoStatus.EndOfFile);
                dropAndClose();
            } else {
                packet.setBuffer(ByteBuffer.wrap(input.getBytes()));
                packet.setStatus(IoStatus.Complete);
            }
        } catch (IOException ex) {
            dropAndClose();
            packet.setStatus(IoStatus.SystemError);
        }
    }

    private void doReset(final DiskIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        dropAndClose();
        packet.setStatus(IoStatus.Complete);
    }

    private void dropAndClose() {
        if (_fileName != null) {
            var f = new File(_fileName);
            try {
                Files.deleteIfExists(f.toPath());
            } catch (IOException ex) {
                // nothing to be done
            }
            _fileName = null;
        }

        if (_reader != null) {
            try {
                _reader.close();
            } catch (IOException ex) {
                // nothing can be done
            }
        }
        _reader = null;
    }

    public void terminate() {
        _terminate = true;
    }

    public void run() {
        while (!_terminate) {
            if (!isReady()) {
                var f = new File(_fileSystemPath).listFiles();
                if (f != null) {
                    var fileList = Stream.of(f)
                                         .filter(file -> !file.isDirectory())
                                         .map(File::getName)
                                         .toList();
                    if (!fileList.isEmpty()) {
                        _fileName = _fileSystemPath + fileList.getFirst();
                        try {
                            _reader = new BufferedReader(new FileReader(_fileName));
                        } catch (IOException ex) {
                            _reader = null;
                            _fileName = null;
                        }
                    }
                }
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                    // nothing
                }
            }
        }
    }
}
