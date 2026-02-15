/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

public class DuplicateIdentifierException extends ConfigurationException {

    public DuplicateIdentifierException(
        final String identifier
    ) {
        super("Duplicate identifier '" + identifier + "'");
    }
}
