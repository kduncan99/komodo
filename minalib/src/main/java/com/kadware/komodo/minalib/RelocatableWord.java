/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.baselib.Word36;

/**
 * Contains a value for storage, which might be extended by certain control and/or undefined references.
 */
public class RelocatableWord extends Word36 {

    public final UndefinedReference[] _references;

    /**
     * Constructor
     */
    RelocatableWord(
        final long value,
        final UndefinedReference[] references
    ) {
        super(value);
        _references = references;
    }

    /**
     * Test for equality - works for this object and for Word36
     * @param obj comparison object
     * @return true if the objects are equal, else false
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof Word36) {
            if (obj instanceof RelocatableWord) {
                if (!UndefinedReference.equals(_references, ((RelocatableWord) obj)._references)) {
                    return false;
                }
            }

            return (((Word36) obj).getW() == getW());
        }
        return false;
    }
}
