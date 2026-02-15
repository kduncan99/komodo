/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

public abstract class KExecException extends Exception {

    public KExecException() {}

    public KExecException(final String message) {
        super(message);
    }

    public KExecException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
