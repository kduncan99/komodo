/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.exceptions;

/**
 * Exception thrown by a method when it is asked to do perform a floating point operation which would
 * result in a characteristic overflow.
 */
public class CharacteristOverflowException extends Exception {

    public CharacteristOverflowException() {}
}
