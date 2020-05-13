/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.exceptions;

/**
 * Thrown when some operation is requested (usually on a value or values)
 * which cannot be completed due to incompatibilities in relocation information.
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
