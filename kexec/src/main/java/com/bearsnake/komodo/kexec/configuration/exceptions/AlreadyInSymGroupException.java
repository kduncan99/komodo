/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

public class AlreadyInSymGroupException extends ConfigurationException {

    public AlreadyInSymGroupException(
        final String identifier
    ) {
        super("'Identifier '" + identifier + "' is already in a SYMGROUP");
    }
}
