/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.values;

import com.bearsnake.komodo.baselib.Parser;

public class BooleanValue implements Value {

    public static final BooleanValue TRUE = new BooleanValue(true);
    public static final BooleanValue FALSE = new BooleanValue(false);

    private final boolean _value;

    public BooleanValue(
        final boolean value
    ) {
        _value = value;
    }

    public boolean getValue() { return _value; }

    @Override
    public ValueType getValueType() { return ValueType.BOOLEAN; }

    @Override
    public String toString() {
        return _value ? "TRUE" : "FALSE";
    }

    public static BooleanValue parse(
        final Parser parser
    ) {
        if (parser.parseToken("true") || parser.parseToken("TRUE")) {
            return new BooleanValue(Boolean.TRUE);
        } else if (parser.parseToken("false") || parser.parseToken("FALSE")) {
            return new BooleanValue(Boolean.FALSE);
        } else {
            return null;
        }
    }
}
