/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * This object is for wrapping block counts - they could be Word36 blocks or byte blocks - we don't care.
 */
public class BlockCount extends Counter {

    public BlockCount() { }
    public BlockCount(long value) { super(value); }
}
