/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is asked to add a device to an channel module
 * at an address which already contains some other device.
 */
public class DeviceIndexConflictException extends Exception {

    public DeviceIndexConflictException(
        final int devIndex
    ) {
        super(String.format("Device index conflict %d", devIndex));
    }
}
