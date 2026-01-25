/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

public abstract class KuteException extends Exception {

    public KuteException() {}

    public KuteException(String message) {
        super(message);
    }
}
