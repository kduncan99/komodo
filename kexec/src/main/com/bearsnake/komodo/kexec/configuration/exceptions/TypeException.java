/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

public class TypeException extends ConfigurationException {

    public TypeException(
        final Object value,
        final Class<?> expectedClass
    ) {
        super(String.format("Value %s is of the wrong type %s; expected type %s",
                            value.toString(), value.getClass(), expectedClass));
    }
}
