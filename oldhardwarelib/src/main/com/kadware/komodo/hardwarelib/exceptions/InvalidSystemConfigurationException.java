/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when the system configuration is invalid, and some attempt is made to invoke one function or another
 * which cannot proceed in said configuration.
 */
public class InvalidSystemConfigurationException extends Exception {

    public InvalidSystemConfigurationException(
    ) {
        super(String.format("Invalid System Configuration"));
    }
}
