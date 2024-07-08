/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.configuration.values;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.configuration.exceptions.SyntaxException;

public class CharacterValue extends Value {

    public static CharacterValue NUL = new CharacterValue('\0');

    private final char _value;

    public CharacterValue(
        final char value
    ) {
        _value = value;
    }

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof CharacterValue val) && (_value == val._value);
    }

    @Override
    public int hashCode() {
        return _value;
    }

    public char getValue() { return _value; }

    @Override
    public ValueType getValueType() { return ValueType.CHARACTER; }

    @Override
    public String toString() {
        return "'" + _value + "'";
    }

    public static CharacterValue parse(
        final Parser parser
    ) throws SyntaxException {
        if (!parser.parseChar('\'')) {
            return null;
        }

        var ch = parser.next();
        if (ch == '\'') {
            if (!parser.atEnd()) {
                throw new SyntaxException();
            }

            return new CharacterValue('\0'); // empty character string - use NUL byte
        }

        if (!parser.parseChar('\'')) {
            throw new SyntaxException("Missing character terminator");
        }

        return new CharacterValue(ch);
    }
}
