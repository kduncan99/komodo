/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration;

public class IntegerRangeRestriction implements ParameterRestriction {

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
    public boolean isValueAcceptable(final Object value) {
        if (value instanceof Integer i) {
            return i >= _min && i <= _max;
        } else {
            return false;
        }
    }
}
