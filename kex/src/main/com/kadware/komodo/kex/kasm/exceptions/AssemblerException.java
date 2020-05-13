/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.exceptions;

import com.kadware.komodo.kex.exceptions.KexException;

/**
 * Base class for all kasm exceptions
 */
public abstract class AssemblerException extends KexException {

    public AssemblerException() {}

    public AssemblerException(
        final String message
    ) {
        super(message);
    }
}
