/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.exceptions;

public abstract class EngineException extends Exception {

    public EngineException() {}
    public EngineException(final String message) { super(message); }
}
