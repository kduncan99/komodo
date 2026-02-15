/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.IntegerRangeException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.IntegerValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class IntegerRangeRestriction implements Restriction {

    private final int _min;
    private final int _max;

    public IntegerRangeRestriction(
        final int min,
        final int max
    ) {
        _min = min;
        _max = max;
    }

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof IntegerValue iv) {
            if ((iv.getValue() < _min) || (iv.getValue() > _max)) {
                throw new IntegerRangeException(iv.getValue(), this);
            }
        } else {
            throw new ValueTypeException(value, ValueType.INTEGER);
        }
    }

    @Override
    public String toString() {
        return String.format("[%d:%d]", _min, _max);
    }
}
