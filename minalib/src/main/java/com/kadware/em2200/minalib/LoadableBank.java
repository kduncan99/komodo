/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.Word36Array;

/**
 * Represents a loadable bank, part of an AbsoluteModule object.
 */
public class LoadableBank {

    public final int _bankDescriptorIndex;
    public final String _bankName;
    public final int _startingAddress;
    public final Word36Array _content;

    /**
     * Constructor
     * @param bdi bank descriptor index
     */
    public LoadableBank(
        final int bdi,
        final String name,
        final int startingAddress,
        final Word36Array content
    ) {
        _bankDescriptorIndex = bdi;
        _bankName = name;
        _startingAddress = startingAddress;
        _content = content;
    }
}
