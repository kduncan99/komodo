/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.jsonlib.exceptions.DataFormatJsonException;
import com.kadware.komodo.jsonlib.exceptions.NotFoundJsonException;
import com.kadware.komodo.parserlib.Parser;
import com.kadware.komodo.parserlib.exceptions.NotFoundParserException;
import com.kadware.komodo.parserlib.exceptions.OutOfDataParserException;
import java.util.Arrays;
import java.util.LinkedList;

public class JsonArray implements JsonEntity {

    public final JsonEntity[] _value;

    public JsonArray(
        final JsonEntity[] value
    ) {
        _value = Arrays.copyOf(value, value.length);
    }

    @Override public boolean isArray() { return true; }
    @Override public boolean isBoolean() { return false; }
    @Override public boolean isComponent() { return false; }
    @Override public boolean isComposite() { return true; }
    @Override public boolean isNull() { return false; }
    @Override public boolean isNumber() { return false; }
    @Override public boolean isObject() { return false; }
    @Override public boolean isString() { return false; }

    public static JsonArray deserialize(
        final Parser parser
    ) throws DataFormatJsonException {
        try {
            //  Are we in an array?
            if (parser.atEnd() || (parser.peek() != '[')) {
                return null;
            }
            parser.skip(1);

            //  We're in an array
            boolean acceptEntity = true;
            boolean acceptTerminator = true;
            boolean acceptComma = false;
            boolean done = false;
            LinkedList<JsonEntity> entities = new LinkedList<>();
            while (!done) {
                parser.skipWhiteSpace();
                if (parser.atEnd()) {
                    //  Not good - unterminated parser
                    throw new DataFormatJsonException("Unterminated array in input");
                }

                if (acceptTerminator && (parser.peek() == ']')) {
                    parser.skip(1);
                    done = true;
                    continue;
                }

                if (acceptComma && (parser.peek() == ',')) {
                    parser.skip(1);
                    acceptEntity = true;
                    acceptComma = false;
                    acceptTerminator = false;
                    continue;
                }

                try {
                    entities.add(JsonParser.subParse(parser));
                    acceptComma = true;
                    acceptEntity = false;
                    acceptTerminator = true;
                } catch (NotFoundJsonException ex) {
                    throw new DataFormatJsonException("No JSON entity found where one was expected");
                }
            }

            return new JsonArray(entities.toArray(new JsonEntity[0]));
        } catch (OutOfDataParserException ex) {
            //  This is generally an internal error, as we make efforts to avoid this as a spurious case
            return null;//TODO what to do
        }
    }

    @Override
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (JsonEntity entity : _value) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(entity.serialize());
        }
        return sb.toString();
    }

    @Override
    public String[] serializeForDisplay() {
        String[] result;
        if (_value.length == 0) {
            result = new String[1];
            result[0] = "[]";
        } else if (_value.length == 1) {
            String[] subResult = _value[0].serializeForDisplay();
            result = new String[subResult.length];
            for (int rx = 0; rx < subResult.length; ++rx) {
                boolean first = rx == 0;
                boolean last = rx == subResult.length - 1;
                String prefix = first ? "[ " : "  ";
                String suffix = last ? " ]" : ",";
                result[rx] = String.format("%s%s%s", prefix, subResult[rx], suffix);
            }
        } else {
            String[] subResult = _value[0].serializeForDisplay();
            result = new String[subResult.length + 2];
            result[0] = "[";
            for (int srx = 0, rx = 1; srx < subResult.length; ++srx, ++rx) {
                boolean last = rx == subResult.length - 1;
                String suffix = last ? "," : "";
                result[rx] = String.format("  %s%s", subResult[srx], suffix);
            }
            result[result.length - 1] = "]";
        }

        return result;
    }
}
