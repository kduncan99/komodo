/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.jsonlib;

import com.kadware.komodo.jsonlib.exceptions.DataFormatJsonException;
import com.kadware.komodo.jsonlib.exceptions.NotFoundJsonException;
import com.kadware.komodo.parserlib.Parser;
import java.io.InputStream;
import java.nio.charset.Charset;

public class JsonParser {

    public static JsonEntity parse(
        final String text
    ) throws DataFormatJsonException,
             NotFoundJsonException {
        return parse(new Parser(text));
    }

    public static JsonEntity parse(
        final byte[] bytes
    ) throws DataFormatJsonException,
             NotFoundJsonException {
        return parse(new Parser(bytes, Charset.defaultCharset()));
    }

    public static JsonEntity parse(
        final InputStream inputStream
    ) throws DataFormatJsonException,
             NotFoundJsonException {
        return parse(new Parser(inputStream, Charset.defaultCharset()));
    }

    public static JsonEntity parse(
        final Parser parser
    ) throws DataFormatJsonException,
             NotFoundJsonException {
        JsonEntity entity = subParse(parser);
        parser.skipWhiteSpace();
        if (!parser.atEnd()) {
            throw new DataFormatJsonException("Valid JSON was followed by extraneous character(s)");
        }
        return entity;
    }

    static JsonEntity subParse(
        final Parser parser
    ) throws NotFoundJsonException {
        parser.skipWhiteSpace();
        if (parser.atEnd()) {
            throw new NotFoundJsonException("Input was empty, or contained only whitespace");
        }

        JsonEntity entity = JsonNull.deserialize(parser);
        if (entity == null) {
            entity = JsonBoolean.deserialize(parser);
        }

        if (entity == null) {
            throw new NotFoundJsonException("Input was not recognized as any form of JSON");
        }

        return entity;
    }
}
