/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.exceptions;

import java.io.IOException;

/**
 * Base class for all IO-related exceptions in the kex package
 */
public class KexIOException extends IOException {

    public KexIOException(
        final String message
    ) {
        super(message);
    }
}
