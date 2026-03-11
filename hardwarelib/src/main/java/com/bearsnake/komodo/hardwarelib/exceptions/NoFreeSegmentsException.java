/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.exceptions;

import com.bearsnake.komodo.hardwarelib.processors.MainStorageProcessor;

public class NoFreeSegmentsException extends Exception {

    public NoFreeSegmentsException(
        final MainStorageProcessor processor
    ) {
        super("No free segments available for " + processor.getName());
    }
}
