/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * This object is for wrapping block IDs - they could be Word36 blocks or byte blocks - we don't care.
 */
public class BlockId extends Identifier {

    public BlockId() { }
    public BlockId(long value) { super(value); }
}
