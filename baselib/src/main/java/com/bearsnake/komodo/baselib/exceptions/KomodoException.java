/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib.exceptions;

/**
 * Base class for all exceptions thrown by Komodo code.
 * Various modules may (and probably should) override this.
 */
public abstract class KomodoException extends Exception {

    protected KomodoException() { super(); }
    protected KomodoException(String message) { super(message); }
    protected KomodoException(String message, Throwable cause) { super(message, cause); }
}
