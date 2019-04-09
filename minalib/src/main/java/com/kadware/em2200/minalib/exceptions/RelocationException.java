/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.exceptions;

/**
 * Thrown when some operation is requested (usually on a value or values)
 * which cannot be completed due to incompatabilities in relocation information.
 */
public class RelocationException extends AssemblerException {

    public RelocationException(
    ) {
    }

    public RelocationException(
        final String message
    ) {
        super(message);
    }
}
