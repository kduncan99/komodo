/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.parameters;

import com.bearsnake.komodo.kexec.configuration.exceptions.FixedParameterException;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class FixedConfigParameter extends Parameter {

    public FixedConfigParameter(
        final Tag tag,
        final ValueType valueType,
        final Value defaultValue,
        final String description
    ) {
        super(tag, valueType, defaultValue, description);
        assert(defaultValue != null);
    }

    @Override
    public void setValue(
        final Value value
    ) throws FixedParameterException {
        throw new FixedParameterException();
    }
}
