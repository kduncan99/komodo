/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.exceptions;

import com.bearsnake.komodo.hardwarelib.processors.MainStorageProcessor;

public class InvalidSegmentIndexException extends Exception {

    public InvalidSegmentIndexException(
        final MainStorageProcessor processor,
        final int index
    ) {
        super(String.format("Invalid index %d for %s", index, processor.getName()));
    }
}
