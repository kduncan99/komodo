/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.exceptions;

import com.kadware.em2200.hardwarelib.misc.AbsoluteAddress;

/**
 * Exception thrown when some entity requests a Processor of a specific type by UPI,
 * and the processor associated with that UPI is of a different type.
 */
public class UPIProcessorTypeException extends Exception {

    public UPIProcessorTypeException(
        final short upi,
        final Class clazz
    ) {
        super(String.format("Processor with UPI %d is not an instance of %s", upi, clazz.getName()));
    }
}
