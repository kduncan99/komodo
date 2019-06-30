/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.types;

/**
 * This object is for wrapping block counts - they could be Word36 blocks or byte blocks - we don't care.
 */
public class BlockCount extends Counter {

    /**
     * Default constructor
     */
    public BlockCount(
    ) {
    }

    /**
     * Constructor
     * <p>
     * @param value
     */
    public BlockCount(
        final long value
    ) {
        super(value);
    }
}
