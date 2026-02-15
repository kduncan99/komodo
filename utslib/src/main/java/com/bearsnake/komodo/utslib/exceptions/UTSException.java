/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.exceptions;

import com.bearsnake.komodo.baselib.exceptions.KomodoException;

public abstract class UTSException extends KomodoException {

    protected UTSException() { super(); }
    protected UTSException(final String message) { super(message); }
    protected UTSException(final String message, final Throwable cause) { super(message, cause); }
}
