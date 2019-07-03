/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.nio.ByteBuffer;

/**
 * A super calss for classes which are returned by IOFunction::GetInfo in a byte buffer for byte devices
 */
@SuppressWarnings("Duplicates")
public abstract class DeviceInfo {

    private DeviceModel _deviceModel;
    private DeviceType _deviceType;
    boolean _isReady;
    boolean _unitAttention;

    /**
     * Constructor for sub-class builder
     */
    DeviceInfo(
        final DeviceType type,
        final DeviceModel model
    ) {
        _deviceModel = model;
        _deviceType = type;
        _isReady = false;
        _unitAttention = false;
    }

    /**
     * Constructor for deserializer
     */
    DeviceInfo() {}

    DeviceModel getDeviceModel() { return _deviceModel; }
    DeviceType getDeviceType() { return _deviceType; }
    boolean isReady() { return _isReady; }
    boolean unitAttention() { return _unitAttention; }
    void setIsReady(boolean flag) { _isReady = flag; }
    void set_unitAttention(boolean flag) { _unitAttention = flag; }

    /**
     * Deserializes the pertinent information for this device, to a byte stream.
     * We might be overridden by a subclass, which calls here first, then deserializes its own information.
     *
     * @param buffer source for deserialization
     */
    void deserializeDiskInfo(
        final ByteBuffer buffer
    ) {
        _deviceType = DeviceType.getValue(buffer.getInt());
        _deviceModel = DeviceModel.getValue(buffer.getInt());
        _isReady = Node.deserializeBoolean(buffer);
        _unitAttention = Node.deserializeBoolean(buffer);
    }

    /**
     * Serializes information for this device to a byte stream.
     * We might be overridden by a subclass, which calls here first, then serializes its own information.
     *
     * @param buffer target of serialization
     */
    void serialize(
        final ByteBuffer buffer
    ) {
        buffer.putInt(_deviceType.getCode());
        buffer.putInt(_deviceModel.getCode());
        Node.serializeBoolean(buffer, _isReady);
        Node.serializeBoolean(buffer, _unitAttention);
    }
}
