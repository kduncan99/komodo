/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.exceptions;

import java.io.IOException;

public class TrackNotAllocatedException extends IOException {

    public TrackNotAllocatedException(
        final long trackId
    ) {
        super(String.format("Track %d is not allocated", trackId));
    }
}
