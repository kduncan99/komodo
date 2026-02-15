/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.IntegerMultipleException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.IntegerValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class IntegerMultipleRestriction implements Restriction {

    private final int _factor;

    public IntegerMultipleRestriction(
        final int factor
    ) {
        _factor = factor;
    }

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof IntegerValue iv) {
            if (iv.getValue() % _factor != 0) {
                throw new IntegerMultipleException(value, this);
            }
        } else {
            throw new ValueTypeException(value, ValueType.INTEGER);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(_factor);
    }
}
