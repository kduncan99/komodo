/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

public class SyntaxException extends ConfigurationException {

    public SyntaxException() {
        super("Syntax error");
    }

    public SyntaxException(
        final String message
    ) {
        super(message);
    }
}
