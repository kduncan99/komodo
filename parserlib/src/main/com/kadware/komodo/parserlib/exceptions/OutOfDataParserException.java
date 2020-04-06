/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.parserlib.exceptions;

public class OutOfDataParserException extends ParserException {

    public OutOfDataParserException(
        final int index
    ) {
        super(index, "com.kadware.komodo.parserlib.Parser is out of data");
    }
}

