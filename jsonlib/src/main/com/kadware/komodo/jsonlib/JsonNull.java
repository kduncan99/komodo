/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.jsonlib.exceptions.NotFoundJsonException;
import com.kadware.komodo.parserlib.Parser;
import com.kadware.komodo.parserlib.exceptions.NotFoundParserException;
import com.kadware.komodo.parserlib.exceptions.OutOfDataParserException;

public class JsonNull implements JsonEntity {

    public JsonNull () {}

    @Override public boolean isArray() { return false; }
    @Override public boolean isBoolean() { return false; }
    @Override public boolean isComponent() { return true; }
    @Override public boolean isComposite() { return false; }
    @Override public boolean isNull() { return true; }
    @Override public boolean isNumber() { return false; }
    @Override public boolean isObject() { return false; }
    @Override public boolean isString() { return false; }

    public static JsonNull deserialize(
        final Parser parser
    ) {
        try {
            parser.parseSpecificTokenCaseInsensitive("null");
            return new JsonNull();
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
