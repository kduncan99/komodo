/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.exceptions;

import com.bearsnake.komodo.baselib.exceptions.KomodoException;

public abstract class KuteException extends KomodoException {

    public KuteException() {}

    public KuteException(String message) {
        super(message);
    }
}
