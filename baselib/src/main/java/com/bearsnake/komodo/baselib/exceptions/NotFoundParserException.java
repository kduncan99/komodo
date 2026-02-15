/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib.exceptions;

/**
 * Thrown by the parser when something which we expected to parse, was not found.
 * This is not necessarily an error.
 */
public class NotFoundParserException extends ParserException {

    public NotFoundParserException() {
        super();
    }

    public NotFoundParserException(final String message) {
        super(message);
    }
}
