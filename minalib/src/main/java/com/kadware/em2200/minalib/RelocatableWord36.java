/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.Word36;

/**
 * Contains a value for storage, which might be extended by certain control and/or
 * currently-undefined references.
 */
public class RelocatableWord36 extends Word36 {

    /**
     * Constructor
     */
    public RelocatableWord36(
    ) {
        super();
    }

    /**
     * Constructor
     */
    public RelocatableWord36(
        final long value
    ) {
        super(value);
    }

    /**
     * Constructor
     */
    public RelocatableWord36(
        final Word36 value
    ) {
        super(value.getW());
    }

    /**
     * Test for equality - works for this object and for Word36
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof Word36) {
            Word36 w = (Word36)obj;
            return getW() == w.getW();
        }
        return false;
    }
}
