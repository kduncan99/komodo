/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.parserlib.exceptions;

public class ParserException extends Exception {

    public ParserException (
        final int index,
        final String message
    ) {
        super(String.format("At index %d:%s", index, message));
    }
}
