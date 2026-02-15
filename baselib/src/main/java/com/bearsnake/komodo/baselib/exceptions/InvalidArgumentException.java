/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib.exceptions;

/**
 * For an internal error caused by invalid or out-of-range arguments to some function.
 */
public class InvalidArgumentException extends RuntimeException {

    protected InvalidArgumentException() { super(); }
    public InvalidArgumentException(String message) { super(message); }
    protected InvalidArgumentException(String message, Throwable cause) { super(message, cause); }
}
