/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib.exceptions;

/**
 * Base class for all run-time exceptions thrown by Komodo code.
 * Various modules may (and probably should) override this.
 */
public abstract class KomodoRuntimeException extends RuntimeException {

    protected KomodoRuntimeException() { super(); }
    protected KomodoRuntimeException(String message) { super(message); }
    protected KomodoRuntimeException(String message, Throwable cause) { super(message, cause); }
}
