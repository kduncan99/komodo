/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.values;

import com.bearsnake.komodo.baselib.Parser;

public class FloatValue extends Value {

    public static final FloatValue ZERO = new FloatValue(0.0f);

    private final double _value;
    
    public FloatValue(
        final double value
    ) {
        _value = value;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof FloatValue val) && (_value == val._value);
    }

    @Override
    public int hashCode() {
        return Double.hashCode(_value);
    }

    public double getValue() { return _value; }

    @Override
    public ValueType getValueType() { return ValueType.FLOAT; }

    @Override
    public String toString() {
        return Double.toString(_value);
    }

    public static FloatValue parse(
        final Parser parser
    ) {
        var ch = parser.peekNext();
        if (!Character.isDigit(ch) && (ch != '.')) {
            return null;
        }

        double value = 0.0;
        while (Character.isDigit(parser.peekNext())) {
            ch = parser.next();
            value = value * 10 + (ch - '0');
        }

        if (parser.peekNext() == '.') {
            parser.skipNext();
            double factor = 0.1;
            while (Character.isDigit(parser.peekNext())) {
                ch = parser.next();
                value = value + factor * (ch - '0');
                factor /= 10;
            }
        }

        return new FloatValue(value);
    }
}
