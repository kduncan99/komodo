/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.restrictions;

import com.bearsnake.komodo.kexec.configuration.exceptions.ConfigurationException;
import com.bearsnake.komodo.kexec.configuration.exceptions.RangeException;
import com.bearsnake.komodo.kexec.configuration.exceptions.TypeException;

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
        final Object value
    ) throws ConfigurationException {
        if (value instanceof Integer iv) {
            if ((iv < _min) || (iv > _max)) {
                throw new RangeException(value, this);
            }
        } else if (value instanceof Long lv){
            if ((lv < _min) || (lv > _max)) {
                throw new RangeException(value, this);
            }
        } else {
            throw new TypeException(value, Integer.class);
        }
    }

    @Override
    public String toString() {
        return String.format("[%d:%d]", _min, _max);
    }
}
