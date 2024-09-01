/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.hardwarelib.devices.DeviceInfo;
import com.bearsnake.komodo.hardwarelib.devices.MountInfo;

public abstract class IoPacket {

    private IoFunction _function;
    private IoStatus _status;
    private String _additionalStatus; // only valid if _status is SystemError
    private DeviceInfo _deviceInfo;
    private MountInfo _mountInfo;

    public String getAdditionalStatus() { return _additionalStatus; }
    public DeviceInfo getDeviceInfo() { return _deviceInfo; }
    public IoFunction getFunction() { return _function; }
    public MountInfo getMountInfo() { return _mountInfo; }
    public IoStatus getStatus() { return _status; }
    public IoPacket setAdditionalStatus(final String systemMessage) { _additionalStatus = systemMessage; return this; }
    public IoPacket setDeviceInfo(final DeviceInfo deviceInfo) { _deviceInfo = deviceInfo; return this; }
    public IoPacket setFunction(final IoFunction function) { _function = function; return this; }
    public IoPacket setMountInfo(final MountInfo mountInfo) { _mountInfo = mountInfo; return this; }
    public IoPacket setStatus(final IoStatus status) { _status = status; return this; }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(_function).append(" ").append(_status);
        if (_additionalStatus != null) {
            sb.append(":").append(_additionalStatus);
        }
        if (_deviceInfo != null) {
            sb.append(" ").append(_deviceInfo);
        }
        if (_mountInfo != null) {
            sb.append(" ").append(_mountInfo);
        }
        return sb.toString();
    }
}
