/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.jsonlib.exceptions.NotFoundJsonException;
import com.kadware.komodo.parserlib.Parser;
import com.kadware.komodo.parserlib.exceptions.NotFoundParserException;
import com.kadware.komodo.parserlib.exceptions.OutOfDataParserException;

public class JsonNull implements JsonNode {

    @Override public boolean isArray() { return false; }
    @Override public boolean isComponent() { return true; }
    @Override public boolean isComposite() { return false; }
    @Override public boolean isNull() { return true; }
    @Override public boolean isNumber() { return false; }
    @Override public boolean isObject() { return false; }
    @Override public boolean isString() { return false; }

    @Override
    public void deserialize(
        final Parser parser
    ) throws NotFoundJsonException {
        try {
            parser.parseSpecificTokenCaseInsensitive("null");
        } catch (NotFoundParserException | OutOfDataParserException ex) {
            throw new NotFoundJsonException("null entity could not be parsed from the input:" + ex.getMessage());
        }
    }

    @Override
    public String serialize() {
        return "null";
    }
}
