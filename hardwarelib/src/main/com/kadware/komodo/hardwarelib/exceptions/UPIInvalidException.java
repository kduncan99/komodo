/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when InventoryManager is given a processor with a UPI which is invalid for that processor type
 * (or completely out of range).
 */
public class UPIInvalidException extends Exception {

    public UPIInvalidException(
        final int upiIndex
    ) {
        super(String.format("UPI invalid %d", upiIndex));
    }
}
