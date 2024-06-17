/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

/**
 * This is the base class for all devices which emulate card readers,
 * placing jobs into backlog based on whatever they do.
 */
public abstract class ImageInputDevice extends SymbiontDevice {

    public ImageInputDevice(final String nodeName) {
        super(nodeName);
    }

    public abstract DiskInfo getInfo();
}
