/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.exceptions;

/**
 * Exception thrown by a method when it is asked to do perform a floating point operation which would
 * result in division by zero.
 */
public class DivideByZeroException extends EngineException {

    public DivideByZeroException() {}
}
