/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.exceptions;

import com.bearsnake.komodo.hardwarelib.processors.MainStorageProcessor;

public class SegmentDoesNotExistException extends Exception {

    public SegmentDoesNotExistException(
        final MainStorageProcessor processor,
        final int index
    ) {
        super(String.format("Segment index %d does not exist for %s", index, processor.getName()));
    }
}
