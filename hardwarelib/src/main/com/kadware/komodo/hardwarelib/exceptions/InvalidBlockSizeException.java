/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib.exceptions;

/**
 * Exception thrown by a method when an invalid block size is detected
 */
public class InvalidBlockSizeException extends Exception {

    public InvalidBlockSizeException(
        final long blockSize
    ) {
        super(String.format("Invalid Block Size:%d", blockSize));
    }
}
