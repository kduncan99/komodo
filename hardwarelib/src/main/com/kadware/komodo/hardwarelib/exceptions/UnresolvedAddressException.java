/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown when address resolution code for Basic Mode Indirect Addressing has followed
 * one indirect vector and has more work to do, but has returned to allow pending interrupts (if any)
 * to be serviced.
 */
public class UnresolvedAddressException extends Exception {

    public UnresolvedAddressException(
    ) {
    }
}
