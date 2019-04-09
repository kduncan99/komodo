/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.exceptions;

/**
 * Exception thrown by a method when it is directed to perform a division, and the divisor is zero.
 */
public class DivideByZeroException extends Exception {

    public DivideByZeroException(
        final String message
    ) {
        super(message);
    }
}
