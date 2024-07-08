/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.FloatRangeException;
import com.bearsnake.komodo.kexec.configuration.exceptions.ValueTypeException;
import com.bearsnake.komodo.kexec.configuration.values.FloatValue;
import com.bearsnake.komodo.kexec.configuration.values.Value;
import com.bearsnake.komodo.kexec.configuration.values.ValueType;

public class FloatRangeRestriction implements Restriction {

    private final double _min;
    private final double _max;

    public FloatRangeRestriction(
        final double min,
        final double max
    ) {
        _min = min;
        _max = max;
    }

    @Override
    public void checkValue(
        final Value value
    ) throws ConfigurationException {
        if (value instanceof FloatValue fv) {
            if ((fv.getValue() < _min) || (fv.getValue() > _max)) {
                throw new FloatRangeException(fv.getValue(), this);
            }
        } else {
            throw new ValueTypeException(value, ValueType.FLOAT);
        }
    }

    @Override
    public String toString() {
        return String.format("[%f:%f]", _min, _max);
    }
}
