/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is given a processor with a UPI which is already assigned to another processor
 */
public class UPIConflictException extends Exception {

    public UPIConflictException(
        final short upi
    ) {
        super(String.format("UPI conflict %d", upi));
    }
}
