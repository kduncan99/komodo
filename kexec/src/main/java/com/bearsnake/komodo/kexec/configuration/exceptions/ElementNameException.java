/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

public class ElementNameException extends ConfigurationException {

    public ElementNameException(
        final Object value
    ) {
        super(String.format("Value %s is not a valid element name", value));
    }
}
