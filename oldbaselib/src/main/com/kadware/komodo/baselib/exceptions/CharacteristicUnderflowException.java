/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.exceptions;

/**
 * Exception thrown by a method when it is asked to do perform a floating point operation which would
 * result in a characteristic underflow.
 */
public class CharacteristicUnderflowException extends Exception {

    public CharacteristicUnderflowException() {}
}
