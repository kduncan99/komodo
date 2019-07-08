/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * This object is for wrapping block sizes - they could be Word36 blocks or byte blocks - we don't care.
 */
public class BlockSize extends Size {

    public BlockSize() { }
    public BlockSize(int value) { super(value); }
}
