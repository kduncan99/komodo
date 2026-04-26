/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.interrupts.HardwareCheckInterrupt;

/**
 * A storage manager which manages access to main storage,
 * which is presented and consumed as a set of uniquely identified segments.
 */
public interface StorageManager {

    /**
     * Allocates a new segment of storage
     * @param size The size of the segment in words, from 0 to 0x7FFFFFFF.
     * @return The segment index of the allocated segment, from 0 to 0x7FFFFFFF.
     */
    int allocateSegment(final int size) throws HardwareCheckInterrupt;

    /**
     * Releases all segments
     */
    void clearSegments();

    /**
     * Retrieves the array which contains the indicated segment.
     * @param segment The segment index, from 0 to 0x7FFFFFFF.
     * @return The array containing the segment, or null if the segment is invalid.
     */
    long[] getSegment(final int segment) throws HardwareCheckInterrupt;

    /**
     * Retrieves a word from the indicated segment.
     * @param segment The segment index, from 0 to 0x7FFFFFFF.
     * @param offset The offset within the segment, from 0 to segment size - 1.
     * @return The word at the specified offset, or 0 if the segment is invalid or the offset is out of bounds.
     */
    long getWord(final int segment, final int offset) throws HardwareCheckInterrupt;

    /**
     * Releases the indicated segment.  The segment will no longer be accessible.
     * @param segment The segment index, from 0 to 0x7FFFFFFF.
     */
    void releaseSegment(final int segment) throws HardwareCheckInterrupt;

    /**
     * Sets a word in the indicated segment.
     * @param segment The segment index, from 0 to 0x7FFFFFFF.
     * @param offset The offset within the segment, from 0 to segment size - 1.
     * @param value The word value to set.
     */
    void setWord(final int segment, final int offset, final long value) throws HardwareCheckInterrupt;
}
