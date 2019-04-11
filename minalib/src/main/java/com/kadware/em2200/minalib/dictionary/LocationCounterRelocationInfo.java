/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.FieldDescriptor;
import com.kadware.em2200.minalib.Locale;

/**
 * Represents location counter meta-information relating to a particular word of storage.
 * This indicates that the word is an integer value offset from the start of the indicated location counter.
 */
public class LocationCounterRelocationInfo extends RelocationInfo {

    final int _locationCounterIndex;

    public LocationCounterRelocationInfo(
        final FieldDescriptor fieldDescriptor,
        final int locationCounterIndex
    ) {
        super(fieldDescriptor);
        _locationCounterIndex = locationCounterIndex;
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof LocationCounterRelocationInfo) {
            LocationCounterRelocationInfo lcri = (LocationCounterRelocationInfo) obj;
            return (lcri._locationCounterIndex == _locationCounterIndex)
                && (lcri._fieldDescriptor.equals(_fieldDescriptor));
        }
        return false;
    }

    @Override
    public String toString(
    ) {
        return String.format("+LCB$(%d)", _locationCounterIndex);
    }
}
