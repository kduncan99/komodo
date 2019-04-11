/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.FieldDescriptor;

/**
 * Represents any meta-information relating to a particular word of storage.
 */
public abstract class RelocationInfo {

    /**
     * Indicates the portion of the associated word to which this info applies
     */
    final FieldDescriptor _fieldDescriptor;

    protected RelocationInfo(
        final FieldDescriptor fieldDescriptor
    ) {
        _fieldDescriptor = fieldDescriptor;
    }

    //  Force implementation of override
    public abstract boolean equals(final Object obj);
    public abstract String toString();
}
