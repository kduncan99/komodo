/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.exceptions;

/**
 * Thrown when some operation is requested upon a parameter of the wrong type.
 */
public class TypeException extends AssemblerException {

    public TypeException() {}

    public TypeException(
        final String message
    ) {
        super(message);
    }
}
