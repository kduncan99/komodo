/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when some entity requests a Processor by UPI, and no Processor is assigned to that UPI.
 */
public class UPINotAssignedException extends Exception {

    public UPINotAssignedException(
        final int upiIndex
    ) {
        super(String.format("No Processor is associated with UPI %d", upiIndex));
    }
}
