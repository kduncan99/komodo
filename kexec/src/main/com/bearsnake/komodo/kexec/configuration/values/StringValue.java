/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.values;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.configuration.exceptions.SyntaxException;

import java.util.Objects;

public class StringValue extends Value {

    private final String _value;
    
    public StringValue(
        final String value
    ) {
        _value = value;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof StringValue val) && Objects.equals(_value, val._value);
    }

    @Override
    public int hashCode() {
        return _value.hashCode();
    }

    public String getValue() { return _value; }

    @Override
    public ValueType getValueType() { return ValueType.STRING; }

    @Override
    public String toString() {
        return "\"" + _value + "\"";
    }

    public static StringValue parse(
        final Parser parser
    ) throws SyntaxException {
        if (!parser.parseChar('"')) {
            return null;
        }

        var sb = new StringBuilder();
        while (!parser.atEnd()) {
            var ch = parser.next();
            if (ch == '"') {
                return new StringValue(sb.toString());
            }
            sb.append(ch);
        }

        throw new SyntaxException("Missing string terminator");
    }
}
