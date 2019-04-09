/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.types;

/**
 * This object is for wrapping track IDs - a track being 1792 36-bit words.
 * For IDs, caller should avoid using negative numbers.  We would like to make this unsigned, but Java doens't do unsigned longs.
 */
public class TrackId extends Identifier {

    /**
     * Default constructor
     */
    public TrackId(
    ) {
    }

    /**
     * Constructor
     * <p>
     * @param value
     */
    public TrackId(
        final long value
    ) {
        super(value);
    }
}
