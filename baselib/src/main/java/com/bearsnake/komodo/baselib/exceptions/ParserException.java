/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib.exceptions;

/**
 * Base class for all exceptions thrown by the Parser.
 */
public abstract class ParserException extends KomodoException {

    protected ParserException() { super(); }
    protected ParserException(String message) { super(message); }
    protected ParserException(String message, Throwable cause) { super(message, cause); }
}
