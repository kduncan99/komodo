/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.exceptions;

/**
 * Exception thrown when some entity requests a Processor by UPI, and no Processor is assigned to that UPI.
 */
public class UPINotAssignedException extends Exception {

    public UPINotAssignedException(
        final short upi
    ) {
        super(String.format("No Processor is associated with UPI %d", upi));
    }
}
