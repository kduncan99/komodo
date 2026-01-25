/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public abstract class StreamException extends KuteException {

    public StreamException(String message) {
        super("Error in UTS Stream:" + message);
    }
}
