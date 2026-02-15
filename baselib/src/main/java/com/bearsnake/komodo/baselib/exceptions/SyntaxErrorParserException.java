/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.baselib.exceptions;

/**
 * Thrown by the parser when we've found something expected, but it contains a syntax error.
 * This is always an error.
 */
public class SyntaxErrorParserException extends ParserException {

    public SyntaxErrorParserException() {
        super();
    }

    public SyntaxErrorParserException(final String message) {
        super(message);
    }
}
