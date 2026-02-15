/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.channels;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.hardwarelib.IoFunction;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.devices.DeviceInfo;
import com.bearsnake.komodo.hardwarelib.devices.MountInfo;

/**
 * Describes an IO sent to a Channel.
 * The buffer must completely contain the data to be written, or be large enough to contain the expected data to be read.
 */
public class ChannelIoPacket {

    private int _actualWordCount;
    private String _additionalStatus;
    private ArraySlice _buffer;
    private DeviceInfo _deviceInfo;
    private Long _deviceWordAddress;
    private TransferFormat _format;
    private IoFunction _ioFunction;
    private IoStatus _ioStatus;
    private MountInfo _mountInfo;
    private int _nodeIdentifier;
    private Integer _symbiontSpacing;

    public ChannelIoPacket() {
        _ioStatus = IoStatus.NotStarted;
    }

    public final int getActualWordCount() { return _actualWordCount; }
    public final String getAdditionalStatus() { return _additionalStatus; }
    public final ArraySlice getBuffer() { return _buffer; }
    public final DeviceInfo getDeviceInfo() { return _deviceInfo; }
    public final Long getDeviceWordAddress() { return _deviceWordAddress; }
    public final TransferFormat getFormat() { return _format; }
    public final IoFunction getIoFunction() { return _ioFunction; }
    public final IoStatus getIoStatus() { return _ioStatus; }
    public final MountInfo getMountInfo() { return _mountInfo; }
    public final int getNodeIdentifier() { return _nodeIdentifier; }
    public final Integer getSymbiontSpacing() { return _symbiontSpacing; }

    public final ChannelIoPacket setActualWordCount(int actualWordCount) { _actualWordCount = actualWordCount; return this; }
    public final ChannelIoPacket setAdditionalStatus(final String additionalStatus) { _additionalStatus = additionalStatus; return this; }
    public final ChannelIoPacket setBuffer(final ArraySlice buffer) { _buffer = buffer; return this; }
    public final ChannelIoPacket setDeviceInfo(final DeviceInfo deviceInfo) { _deviceInfo = deviceInfo; return this; }
    public final ChannelIoPacket setDeviceWordAddress(final Long deviceWordAddress) { _deviceWordAddress = deviceWordAddress; return this; }
    public final ChannelIoPacket setIoFunction(final IoFunction ioFunction) { _ioFunction = ioFunction; return this; }
    public final ChannelIoPacket setIoStatus(final IoStatus ioStatus) { _ioStatus = ioStatus; return this; }
    public final ChannelIoPacket setFormat(final TransferFormat format) { _format = format; return this; }
    public final ChannelIoPacket setMountInfo(final MountInfo mountInfo) { _mountInfo = mountInfo; return this; }
    public final ChannelIoPacket setNodeIdentifier(final int nodeIdentifier) { _nodeIdentifier = nodeIdentifier; return this; }
    public final ChannelIoPacket setSymbiontSpacing(Integer spacing) { _symbiontSpacing = spacing; return this; }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("[node:").append(_nodeIdentifier).append(" ").append(_ioFunction);
        if (_ioFunction == IoFunction.Read || _ioFunction == IoFunction.ReadBackward || _ioFunction == IoFunction.Write) {
            sb.append(" len:").append(_buffer.getSize());
            if (_deviceWordAddress != null) {
                sb.append(" drwa:").append(_deviceWordAddress);
            }
            sb.append(" fmt:").append(_format);
        }

        sb.append(" status:").append(_ioStatus);
        if (_additionalStatus != null) {
            sb.append(":").append(_additionalStatus);
        }
        sb.append(" xfrSz:").append(_actualWordCount);

        if (_mountInfo != null) {
            sb.append(" mnt:").append(_mountInfo);
        }
        if (_deviceInfo != null) {
            sb.append(" devInfo:").append(_deviceInfo);
        }
        if (_symbiontSpacing != null) {
            sb.append(" spacing:").append(_symbiontSpacing);
        }

        sb.append("]");
        return sb.toString();
    }
}
