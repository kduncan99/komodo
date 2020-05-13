/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.exceptions;

/**
 * Base class for all exceptions in the kex package, or any subordinate packages
 */
public class KexException extends Exception {

    public KexException() {}

    public KexException(
        final String message
    ) {
        super(message);
    }
}
