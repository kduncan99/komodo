/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib.types;

/**
 * This object is for wrapping track IDs - a track being 1792 36-bit words.
 * Caller should avoid using negative numbers.  We would like to make this unsigned, but Java doens't do unsigned longs.
 */
public class TrackCount extends Counter {

    /**
     * Default constructor
     */
    public TrackCount(
    ) {
    }

    /**
     * Constructor
     * <p>
     * @param value
     */
    public TrackCount(
        final long value
    ) {
        super(value);
    }
}
