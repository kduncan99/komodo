/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.parserlib.exceptions;

public class StackEmptyParserException extends ParserException {

    public StackEmptyParserException(
        final int index
    ) {
        super(index, "Stack is empty");
    }
}

