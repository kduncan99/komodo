/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.exceptions;

/**
 * Base class for all mina exceptions
 */
public abstract class AssemblerException extends Exception {

    public AssemblerException(
    ) {
    }

    public AssemblerException(
        final String message
    ) {
        super(message);
    }
}
