/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.parserlib.Parser;
import com.kadware.komodo.parserlib.exceptions.NotFoundParserException;
import com.kadware.komodo.parserlib.exceptions.OutOfDataParserException;

public class JsonBoolean implements JsonEntity {

    public final boolean _value;

    public JsonBoolean(
        final boolean value
    ) {
        _value = value;
    }

    @Override public boolean isArray() { return false; }
    @Override public boolean isBoolean() { return true; }
    @Override public boolean isComponent() { return true; }
    @Override public boolean isComposite() { return false; }
    @Override public boolean isNull() { return false; }
    @Override public boolean isNumber() { return false; }
    @Override public boolean isObject() { return false; }
    @Override public boolean isString() { return false; }

    public static JsonBoolean deserialize(
        final Parser parser
    ) {
        try {
            parser.parseSpecificTokenCaseInsensitive("true");
            return new JsonBoolean(true);
        } catch (NotFoundParserException ex) {
            //  Drop through
        } catch (OutOfDataParserException ex) {
            return null;
        }

        try {
            parser.parseSpecificTokenCaseInsensitive("false");
            return new JsonBoolean(false);
        } catch (NotFoundParserException | OutOfDataParserException ex) {
            return null;
        }
    }

    @Override
    public String serialize() {
        return "null";
    }

    @Override
    public String[] serializeForDisplay() {
        String[] result = { serialize() };
        return result;
    }
}
