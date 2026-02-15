/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.exceptions;

import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class ValueTypeException extends ConfigurationException {

    public ValueTypeException(
        final Value value,
        final ValueType expectedType
    ) {
        super(String.format("Value %s is of the wrong type %s; expected type %s",
                            value.toString(), value.getValueType(), expectedType));
    }
}
