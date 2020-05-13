/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.exceptions;

import java.io.IOException;

public class FileAlreadyOpenException extends KexIOException {

    public FileAlreadyOpenException(
        final String fileName
    ) {
        super(String.format("File '%s' is already open", fileName));
    }
}
