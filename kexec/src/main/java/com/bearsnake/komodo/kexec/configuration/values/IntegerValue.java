/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.values;

import com.bearsnake.komodo.baselib.Parser;

public class IntegerValue extends Value {

    public static final IntegerValue ZERO = new IntegerValue(0);

    private final long _value;
    private final boolean _wasOctal;
    
    public IntegerValue(
        final long value,
        final boolean wasOctal
    ) {
        _value = value;
        _wasOctal = wasOctal;
    }

    public IntegerValue(
        final long value
    ) {
        this(value, false);
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof IntegerValue val) && (_value == val._value);
    }

    @Override
    public int hashCode() {
        return (int)_value;
    }

    public long getValue() { return _value; }
    public boolean wasOctal() { return _wasOctal; }

    @Override
    public ValueType getValueType() { return ValueType.INTEGER; }

    @Override
    public String toString() {
        return _wasOctal ? String.format("0%o", _value) : String.valueOf(_value);
    }

    public static IntegerValue parse(
        final Parser parser
    ) {
        var ch = parser.peekNext();
        if (!Character.isDigit(ch)) {
            return null;
        }

        int radix = ch == '0' ? 8 : 10;
        long value = 0;
        while (Character.isDigit(parser.peekNext())) {
            ch = parser.next();
            value = value * radix + (ch - '0');
        }

        return new IntegerValue(value, radix == 8);
    }
}
