/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.types.BlockCount;
import com.kadware.komodo.baselib.types.BlockSize;
import java.nio.ByteBuffer;

/**
 * This class (or a subset thereof) is returned by IOFunction::GetInfo in a byte buffer for byte devices
 */
@SuppressWarnings("Duplicates")
public class DiskDeviceInfo extends DeviceInfo {

    private BlockCount _blockCount;
    private BlockSize _blockSize;
    private boolean _isMounted;
    private boolean _isWriteProtected;

    /**
     * Constructor for deserializer
     */
    private DiskDeviceInfo() {}

    /**
     * Initial value constructor for builder
     */
    private DiskDeviceInfo(
        final DeviceModel model,
        final BlockCount blockCount,
        final BlockSize blockSize
    ) {
        super(DeviceType.Disk, model);
        _blockCount = blockCount;
        _blockSize = blockSize;
        _isMounted = false;
        _isWriteProtected = false;
    }

    BlockCount getBlockCount() { return _blockCount; }
    BlockSize getBlockSize() { return _blockSize; }
    boolean isMounted() { return _isMounted; }
    boolean is_isWriteProtected() { return _isWriteProtected; }
    void setIsMounted(boolean flag) { _isMounted = flag; }
    void setIsWriteProtected(boolean flag) { _isWriteProtected = flag; }

    /**
     * Deserializes the pertinent information for this disk device, to a byte stream.
     * We might be overridden by a subclass, which calls here first, then deserializes its own information.
     */
    private void deserializeDiskDeviceInfo(
        final ByteBuffer buffer
    ) {
        super.deserializeDiskInfo(buffer);
        _blockCount.deserialize(buffer);
        _blockSize.deserialize(buffer);
        _isMounted = Node.deserializeBoolean(buffer);
        _isWriteProtected = Node.deserializeBoolean(buffer);
    }

    /**
     * Creator/deserializer
     */
    static DiskDeviceInfo deserialized(
        final ByteBuffer buffer
    ) {
        DiskDeviceInfo ddi = new DiskDeviceInfo();
        ddi.deserializeDiskDeviceInfo(buffer);
        return ddi;
    }

    /**
     * Serializes information for this disk device to a byte stream.
     * We might be overridden by a subclass, which calls here first, then serializes its own information.
     */
    @Override
    void serialize(
        final ByteBuffer buffer
    ) {
        super.serialize(buffer);
        _blockCount.serialize(buffer);
        _blockSize.serialize(buffer);
        Node.serializeBoolean(buffer, _isMounted);
        Node.serializeBoolean(buffer, _isWriteProtected);
    }

    public static class Builder {
        private BlockCount _blockCount;
        private BlockSize _blockSize;
        private DeviceModel _deviceModel;

        public Builder setBlockCount(long value) { _blockCount = new BlockCount(value); return this; }
        public Builder setBlockSize(int value) { _blockSize = new BlockSize(value); return this; }
        public Builder setDeviceMode(DeviceModel value) { _deviceModel = value; return this; }

        public DiskDeviceInfo build() {
            return new DiskDeviceInfo(_deviceModel, _blockCount, _blockSize);
        }
    }
}
