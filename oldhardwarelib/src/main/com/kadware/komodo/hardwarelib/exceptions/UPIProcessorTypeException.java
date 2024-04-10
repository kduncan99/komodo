/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when some entity requests a Processor of a specific type by UPI,
 * and the processor associated with that UPI is of a different type.
 */
public class UPIProcessorTypeException extends Exception {

    public UPIProcessorTypeException(
        final int upiIndex,
        final Class clazz
    ) {
        super(String.format("Processor with UPI %d is not an instance of %s", upiIndex, clazz.getName()));
    }
}
