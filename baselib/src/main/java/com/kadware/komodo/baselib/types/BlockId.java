/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.types;

/**
 * This object is for wrapping block IDs - they could be Word36 blocks or byte blocks - we don't care.
 */
public class BlockId extends Identifier {

    /**
     * Default constructor
     */
    public BlockId(
    ) {
    }

    /**
     * Constructor
     * <p>
     * @param value
     */
    public BlockId(
        final long value
    ) {
        super(value);
    }
}
