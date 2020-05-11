/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.exceptions;

import java.io.IOException;

public class ParameterException extends IOException {

    public ParameterException(
        final String message
    ) {
        super(message);
    }
}
