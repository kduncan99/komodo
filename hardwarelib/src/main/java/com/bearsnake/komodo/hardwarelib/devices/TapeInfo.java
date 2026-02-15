/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

public class TapeInfo extends DeviceInfo {

    public TapeInfo(final boolean isMounted,
                    final boolean isReady,
                    final boolean isWriteProtected) {
        super(isMounted, isReady, isWriteProtected);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
