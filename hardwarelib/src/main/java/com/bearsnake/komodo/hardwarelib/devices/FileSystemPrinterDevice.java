/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileSystemPrinterDevice extends SymbiontPrinterDevice {

    public static final Logger LOGGER = LogManager.getLogger(FileSystemPrinterDevice.class);

    private final String _fileSystemPath;
    private boolean _isReady = false;
    private boolean _topOfPage = false;
    private PrintStream _printer = null;

    public FileSystemPrinterDevice(
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
    public final void probe() {}

    @Override
    public final void setIsReady(final boolean flag) {
        _isReady = flag;
    }

    @Override
    public synchronized void performIo(final IoPacket packet) {
        if (_logIos) {
            LOGGER.trace("{}:performIo enter({})", _nodeName, packet.toString());
        }

        if (packet instanceof SymbiontIoPacket symbiontPacket) {
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
            LOGGER.trace("{}:performIo exit({})", _nodeName, packet.toString());
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
        packet.setStatus(IoStatus.Successful);
    }

    private void doStartFile(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_printer != null) {
            close();
        }

        final String fid = packet.getMediaIdentifier();
        if ((fid == null) || fid.isEmpty()) {
            packet.setStatus(IoStatus.InvalidPacket);
            return;
        }

        var dateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String dtStr = dateTime.format(formatter);
        var filename = String.format("%s%s-%s.txt", _fileSystemPath, packet.getMediaIdentifier(), dtStr);
        try {
            _printer = new PrintStream(filename);
            _topOfPage = true;
        } catch (IOException ex) {
            _printer = null;
            packet.setStatus(IoStatus.SystemError).setAdditionalStatus(ex.getMessage());
            return;
        }

        packet.setStatus(IoStatus.Successful);
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
        packet.setStatus(IoStatus.Successful);
    }

    private void doWriteEndOfFile(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_printer != null) {
            _printer.close();
            packet.setStatus(IoStatus.Successful);
        }
    }

    public void close() {
        if (_printer != null) {
            _printer.close();
            _printer = null;
        }
    }
}
