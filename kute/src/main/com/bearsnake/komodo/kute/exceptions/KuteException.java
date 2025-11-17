/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute.exceptions;

public abstract class KuteException extends Exception {

    public KuteException() {}

    public KuteException(String message) {
        super(message);
    }
}
