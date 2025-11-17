/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute.exceptions;

public class ParameterException extends InternalException {

    public ParameterException(String function, String parameter, Object obj) {
        super(String.format("Invalid parameter " + parameter + " in function " + function + " value=" + obj));
    }
}
