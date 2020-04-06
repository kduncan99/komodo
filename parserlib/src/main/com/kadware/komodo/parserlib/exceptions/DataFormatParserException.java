/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.parserlib.exceptions;

public class DataFormatParserException extends ParserException {

    public DataFormatParserException(
        final int index
    ) {
        super(index, "Badly-formatted JSON data");
    }
}

