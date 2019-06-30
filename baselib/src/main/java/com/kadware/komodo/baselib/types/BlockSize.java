/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib.types;

/**
 * This object is for wrapping block sizes - they could be Word36 blocks or byte blocks - we don't care.
 */
public class BlockSize extends Size {

    /**
     * Default constructor
     */
    public BlockSize(
    ) {
    }

    /**
     * Constructor
     * <p>
     * @param value
     */
    public BlockSize(
        final int value
    ) {
        super(value);
    }
}
