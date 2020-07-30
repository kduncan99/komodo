/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is asked to add a device
 * to a Channel Module at an invalid index.
 */
public class InvalidDeviceIndexException extends Exception {

    public InvalidDeviceIndexException(
        final int devIndex
    ) {
        super(String.format("Invalid Device index %d", devIndex));
    }
}
