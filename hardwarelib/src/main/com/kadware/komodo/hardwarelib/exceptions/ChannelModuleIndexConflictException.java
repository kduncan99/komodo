/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is asked to add a channel module to an IOP at an address
 * which already contains a channel module.
 */
public class ChannelModuleIndexConflictException extends Exception {

    public ChannelModuleIndexConflictException(
        final int cmIndex
    ) {
        super(String.format("Channel Module index conflict %d", cmIndex));
    }
}
