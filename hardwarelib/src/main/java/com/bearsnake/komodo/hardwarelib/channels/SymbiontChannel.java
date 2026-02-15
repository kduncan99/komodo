/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.channels;

import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.devices.SymbiontDevice;
import com.bearsnake.komodo.hardwarelib.devices.SymbiontIoPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;

/**
 * Channel for symbiont devices
 */
public class SymbiontChannel extends Channel {

    private static final Logger LOGGER = LogManager.getLogger(SymbiontChannel.class);

    public SymbiontChannel(final String nodeName) {
        super(nodeName);
    }

    @Override
    public boolean canAttach(
        Device device
    ) {
        return device instanceof SymbiontDevice;
    }

    @Override
    public ChannelType getChannelType() {
        return ChannelType.SymbiontChannel;
    }

    @Override
    public void routeIo(final ChannelIoPacket channelPacket) {
        LOGGER.trace("{}:routeIO {}", getNodeName(), channelPacket.toString());

        var nodeId = channelPacket.getNodeIdentifier();
        if (nodeId == getNodeIdentifier()) {
            // There is currently no IO function which we support as a Channel...
            channelPacket.setIoStatus(IoStatus.InvalidFunction);
        } else {
            if (!_devices.containsKey(nodeId)) {
                channelPacket.setIoStatus(IoStatus.DeviceIsNotAttached);
            } else {
                var device = (SymbiontDevice) _devices.get(nodeId);
                switch (channelPacket.getIoFunction()) {
                    case Read -> processRead(device, channelPacket);
                    case Reset -> processReset(device, channelPacket);
                    case Write -> processWrite(device, channelPacket);
                    default -> channelPacket.setIoStatus(IoStatus.InvalidFunction);
                }
            }
        }

        LOGGER.trace("{}:routeIO done:{}", getNodeName(), channelPacket.toString());
    }

    private static IoStatus checkPacket(ChannelIoPacket channelPacket) {
        if ((channelPacket.getFormat() != TransferFormat.QuarterWord)
            && (channelPacket.getFormat() != TransferFormat.SixthWord)) {
            return IoStatus.InvalidTransferFormat;
        }

        if (channelPacket.getBuffer() == null) {
            return IoStatus.BufferIsNull;
        }

        if ((channelPacket.getFormat() == TransferFormat.QuarterWord) && (channelPacket.getBuffer().getSize() > 33)) {
            return IoStatus.InvalidBufferSize;
        } else if ((channelPacket.getFormat() == TransferFormat.SixthWord) && (channelPacket.getBuffer().getSize() > 22)) {
            return IoStatus.InvalidBufferSize;
        }

        return IoStatus.Successful;
    }

    private synchronized void processRead(SymbiontDevice device,
                                          ChannelIoPacket channelPacket) {
        var ioStatus = checkPacket(channelPacket);
        if (ioStatus != IoStatus.Successful) {
            channelPacket.setIoStatus(ioStatus);
            return;
        }

        SymbiontIoPacket ioPacket = new SymbiontIoPacket();
        if (channelPacket.getSymbiontSpacing() != null) {
            ioPacket.setSpacing(channelPacket.getSymbiontSpacing());
        }
        ioPacket.setFunction(IoFunction.Read);

        if (channelPacket.getFormat() == TransferFormat.SixthWord) {
            // Do like below, but translate from ASCII to Fieldata...
            var byteCount = channelPacket.getBuffer().getSize() * 6;
            var ioBuffer = ByteBuffer.allocate(byteCount);
            ioPacket.setBuffer(ioBuffer);

            device.performIo(ioPacket);
            if (ioPacket.getStatus() == IoStatus.Successful) {
                var inputStr = new String(ioBuffer.array());
                if (inputStr.length() % 6 > 0) {
                    inputStr += "     ";
                }
                var inputWordCount = inputStr.length() / 6;

                for (int wx = 0, chx = 0; wx < inputWordCount && wx < channelPacket.getBuffer().getSize(); wx++, chx += 6) {
                    channelPacket.getBuffer().set(wx, Word36.stringToWordFieldata(inputStr.substring(chx, chx + 6)));
                }
                channelPacket.setActualWordCount(channelPacket.getActualWordCount());
            }
        } else if (channelPacket.getFormat() == TransferFormat.QuarterWord) {
            device.performIo(ioPacket);
            if (ioPacket.getStatus() == IoStatus.Successful) {
                var maxChars = channelPacket.getBuffer().getSize() * 4;
                var inputChars = ioPacket.getBuffer().limit();
                var actualChars = Math.min(maxChars, inputChars);
                var actualWords = actualChars / 4;
                if (actualChars % 4 > 0) {
                    actualWords++;
                }

                if (inputChars > maxChars) {
                    ioPacket.setStatus(IoStatus.ReadOverrun);
                }

                channelPacket.getBuffer().unpackQuarterWords(ioPacket.getBuffer().array(), 0, actualChars);
                channelPacket.setActualWordCount(actualWords);
            }
        }

        channelPacket.setIoStatus(ioPacket.getStatus())
                     .setAdditionalStatus(ioPacket.getAdditionalStatus());
    }

    private synchronized void processReset(SymbiontDevice device,
                                           ChannelIoPacket channelPacket) {
        var ioPacket = (SymbiontIoPacket) new SymbiontIoPacket().setFunction(channelPacket.getIoFunction());
        device.performIo(ioPacket);
        channelPacket.setIoStatus(ioPacket.getStatus())
                     .setAdditionalStatus(ioPacket.getAdditionalStatus());
    }

    private synchronized void processWrite(SymbiontDevice device,
                                           ChannelIoPacket channelPacket) {
        var ioStatus = checkPacket(channelPacket);
        if (ioStatus != IoStatus.Successful) {
            channelPacket.setIoStatus(ioStatus);
            return;
        }

        SymbiontIoPacket ioPacket = new SymbiontIoPacket();
        ioPacket.setSpacing(channelPacket.getSymbiontSpacing())
                .setFunction(IoFunction.Write);

        if (channelPacket.getFormat() == TransferFormat.SixthWord) {
            // Do like below, but translate from Fieldata to ASCII...
            var byteArray = Word36.toStringFromFieldata(channelPacket.getBuffer()).getBytes();
            var ioBuffer = ByteBuffer.wrap(byteArray);
            ioPacket.setBuffer(ioBuffer);
            device.performIo(ioPacket);
        } else if (channelPacket.getFormat() == TransferFormat.QuarterWord) {
            var byteCount = channelPacket.getBuffer().getSize() * 4;
            var ioBuffer = ByteBuffer.allocate(byteCount);
            ioPacket.setBuffer(ioBuffer);

            channelPacket.getBuffer().packQuarterWords(ioPacket.getBuffer().array());
            device.performIo(ioPacket);
        }

        if (ioPacket.getStatus() == IoStatus.Successful) {
            channelPacket.setActualWordCount(channelPacket.getActualWordCount());
        }
        channelPacket.setIoStatus(ioPacket.getStatus())
                     .setAdditionalStatus(ioPacket.getAdditionalStatus());
    }
}
