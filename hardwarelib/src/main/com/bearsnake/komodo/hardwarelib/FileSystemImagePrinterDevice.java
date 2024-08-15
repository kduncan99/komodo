/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.logger.LogManager;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileSystemImagePrinterDevice extends SymbiontPrinterDevice {

    private final String _fileSystemPath;
    private boolean _isReady = false;
    private boolean _topOfPage = false;
    private PrintStream _printer = null;

    public FileSystemImagePrinterDevice(
        final String nodeName,
        final String fileSystemPath // path of directory we watch for input files
    ) {
        super(nodeName);
        _fileSystemPath = fileSystemPath.endsWith("/") ? fileSystemPath : fileSystemPath + '/';
    }

    @Override
    public final DeviceModel getDeviceModel() {
        return DeviceModel.FileSystemImagePrinter;
    }

    @Override
    public final boolean isReady() {
        return _isReady;
    }

    @Override
    public final void setIsReady(final boolean flag) {
        _isReady = flag;
    }

    @Override
    public synchronized void startIo(final IoPacket packet) {
        if (_logIos) {
            LogManager.logTrace(_nodeName, "startIo(%s)", packet.toString());
        }

        if (packet instanceof SymbiontIoPacket symbiontPacket) {
            packet.setStatus(IoStatus.InProgress);
            switch (packet.getFunction()) {
                case Reset -> doReset(symbiontPacket);
                case StartFile -> doStartFile(symbiontPacket);
                case Write -> doWrite(symbiontPacket);
                case WriteEndOfFile -> doWriteEndOfFile(symbiontPacket);
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

    private void doReset(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        close();
        packet.setStatus(IoStatus.Complete);
    }

    private void doStartFile(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_printer != null) {
            close();
        }

        final String fid = packet.getIdentifier();
        if ((fid == null) || fid.isEmpty()) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        var dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String dtStr = dateTime.format(formatter);
        var filename = String.format("%s%s-%s.txt", _fileSystemPath, packet.getIdentifier(), dtStr);
        try {
            _printer = new PrintStream(filename);
            _topOfPage = true;
        } catch (IOException ex) {
            _printer = null;
            packet.setStatus(IoStatus.SystemError).setAdditionalStatus(ex.getMessage());
            return;
        }

        packet.setStatus(IoStatus.Complete);
    }

    private void doWrite(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (packet.getSpacing() < 0) {
            _printer.printf("%c", 0x0C); // Form Feed
            _topOfPage = false;
        } else if (packet.getSpacing() > 0) {
            var spacing = packet.getSpacing();
            if (_topOfPage) {
                spacing--;
            }
            for (int nlx = 0; nlx < spacing; nlx++) {
                _printer.println();
            }
        }

        _printer.print(new String(packet.getBuffer().array()));
        packet.setStatus(IoStatus.Complete);
    }

    private void doWriteEndOfFile(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_printer != null) {
            _printer.close();
            packet.setStatus(IoStatus.Complete);
        }
    }

    public void close() {
        if (_printer != null) {
            _printer.close();
            _printer = null;
        }
    }
}
