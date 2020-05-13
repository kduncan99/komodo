/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.exceptions;

/**
 * Base class for all IO-related exceptions in the kex package
 */
public class KexIOException extends Exception {

    public KexIOException(
        final String message
    ) {
        super(message);
    }
}
