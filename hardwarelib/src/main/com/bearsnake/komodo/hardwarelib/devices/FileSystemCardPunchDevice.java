/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import com.bearsnake.komodo.hardwarelib.IoPacket;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.logger.LogManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileSystemCardPunchDevice extends SymbiontPunchDevice {

    private final String _fileSystemPath;
    private boolean _isReady = false;
    private BufferedWriter _writer = null;

    public FileSystemCardPunchDevice(
        final String nodeName,
        final String fileSystemPath // path of directory we watch for input files
    ) {
        super(nodeName);
        _fileSystemPath = fileSystemPath.endsWith("/") ? fileSystemPath : fileSystemPath + '/';
    }

    @Override
    public final DeviceModel getDeviceModel() {
        return DeviceModel.FileSystemImageWriter;
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
            LogManager.logTrace(_nodeName, "startIo(%s)", packet.toString());
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
        packet.setStatus(IoStatus.Successful);
    }

    private void doStartFile(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        if (_writer != null) {
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
            _writer = new BufferedWriter(new FileWriter(filename));
        } catch (IOException ex) {
            _writer = null;
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

        try {
            _writer.write(new String(packet.getBuffer().array()));
            _writer.newLine();
            packet.setStatus(IoStatus.Successful);
        } catch (IOException ex) {
            close();
            packet.setStatus(IoStatus.SystemError).setAdditionalStatus(ex.getMessage());
        }
    }

    private void doWriteEndOfFile(final SymbiontIoPacket packet) {
        if (!isReady()) {
            packet.setStatus(IoStatus.DeviceIsNotReady);
            return;
        }

        close();
        packet.setStatus(IoStatus.Successful);
    }

    @Override
    public void close() {
        if (_writer != null) {
            try {
                _writer.close();
            } catch (IOException ex) {
                // nothing can be done
            }
        }
        _writer = null;
    }
}
