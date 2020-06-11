/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is given a processor with a UPI which is already assigned to another processor
 */
public class UPIConflictException extends Exception {

    public UPIConflictException(
        final int upiIndex
    ) {
        super(String.format("UPI conflict %d", upiIndex));
    }
}
