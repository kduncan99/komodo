/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute.exceptions;

public abstract class InternalException extends KuteException {

    public InternalException(String message) {
        super("Internal Error:" + message);
    }
}
