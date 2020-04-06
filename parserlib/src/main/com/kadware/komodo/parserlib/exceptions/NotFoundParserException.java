/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.parserlib.exceptions;

public class NotFoundParserException extends ParserException {

    public NotFoundParserException(
        final int index
    ) {
        super(index, "Requested entity not found");
    }
}

