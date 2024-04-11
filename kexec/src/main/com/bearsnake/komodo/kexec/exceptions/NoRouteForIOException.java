/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exceptions;

public class NoRouteForIOException extends KExecException {

    public NoRouteForIOException(final long nodeIdentifier) {
        super(String.format("No route to device %d", nodeIdentifier));
    }
}
