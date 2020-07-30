/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is asked to add a channel module to an IOP
 * at an invalid index.
 */
public class InvalidChannelModuleIndexException extends Exception {

    public InvalidChannelModuleIndexException(
        final int cmIndex
    ) {
        super(String.format("Invalid Channel Module index %d", cmIndex));
    }
}
