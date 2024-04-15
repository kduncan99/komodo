/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public abstract class DiskDevice extends Device {

    public DiskDevice(final String nodeName) {
        super(nodeName);
    }

    @Override
    public final DeviceType getDeviceType() {
        return DeviceType.DiskDevice;
    }

    public abstract DiskInfo getInfo();

    public static int getBlockSizeForPrepFactor(final int prepFactor) {
        return switch (prepFactor) {
            case 28 -> 128;
            case 56 -> 256;
            case 112 -> 512;
            case 224 -> 1024;
            case 448 -> 2048;
            case 896 -> 4096;
            case 1792 -> 8192;
            default -> 0;
        };
    }

    public static int getPrepFactorForBlockSize(final int bytesPerBlock) {
        return switch (bytesPerBlock) {
            case 128 -> 28;
            case 256 -> 56;
            case 512 -> 112;
            case 1024 -> 224;
            case 2048 -> 448;
            case 4096 -> 896;
            case 8192 -> 1792;
            default -> 0;
        };
    }
}
