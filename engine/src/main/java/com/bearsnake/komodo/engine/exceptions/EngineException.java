/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.exceptions;

import com.bearsnake.komodo.baselib.exceptions.KomodoException;

public abstract class EngineException extends KomodoException {

    public EngineException() {}
    public EngineException(final String message) { super(message); }
}
