/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.processors;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.hardwarelib.exceptions.InvalidSegmentIndexException;
import com.bearsnake.komodo.hardwarelib.exceptions.NoFreeSegmentsException;
import com.bearsnake.komodo.hardwarelib.exceptions.SegmentDoesNotExistException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class MainStorageProcessor extends Processor{

    private ArraySlice[] _segments;
    private final Stack<Integer> _freeSegments = new Stack<>();

    public MainStorageProcessor(
        final String name,
        final String upi,
        final int segmentCount
    ) {
        super(upi, name);
        _freeSegments.clear();
        _segments = new ArraySlice[segmentCount];
        for (int i = 0; i < segmentCount; ++i) {
            _freeSegments.push(i);
        }
    }

    /**
     * Allocates a new segment of the given size.
     * @param size size of segment to allocate, in words
     * @throws NoFreeSegmentsException if no free segments are available
     * @return segment index
     */
    public synchronized int allocateSegment(
        final int size
    ) throws NoFreeSegmentsException {
        if (_freeSegments.isEmpty()) {
            throw new NoFreeSegmentsException(this);
        }

        var sx = _freeSegments.pop();
        var segment = new ArraySlice(new long[size]);
        _segments[sx] = segment;
        return sx;
    }

    public synchronized void clear() {
        var segmentCount = _segments.length;
        _freeSegments.clear();
        _segments = new ArraySlice[segmentCount];
        for (int i = 0; i < segmentCount; ++i) {
            _freeSegments.push(i);
        }
    }

    /**
     * Gets the allocated segment indexes.
     * Useful for producing memory dumps.
     * @return collection of allocated segment indexes
     */
    public synchronized Collection<Integer> getAllocatedSegmentIndexes() {
        return IntStream.range(0, _segments.length)
                        .filter(i -> _segments[i] != null)
                        .boxed()
                        .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * Gets the segment at the given index.
     * @param segmentIndex index of segment to get
     * @throws InvalidSegmentIndexException if index is out of bounds
     * @throws SegmentDoesNotExistException if segment does not exist
     * @return segment at index
     */
    public synchronized ArraySlice getSegment(
        final int segmentIndex
    ) throws InvalidSegmentIndexException,
             SegmentDoesNotExistException {
        if ((segmentIndex < 0) || (segmentIndex >= _segments.length)) {
            throw new InvalidSegmentIndexException(this, segmentIndex);
        }

        var segment = _segments[segmentIndex];
        if (segment == null) {
            throw new SegmentDoesNotExistException(this, segmentIndex);
        }

        return segment;
    }

    /**
     * Frees the segment at the given index.
     * @param segmentIndex index of segment to free
     * @throws InvalidSegmentIndexException if index is out of bounds
     * @throws SegmentDoesNotExistException if segment does not exist
     */
    public synchronized void releaseSegment(
        final int segmentIndex
    ) throws InvalidSegmentIndexException,
             SegmentDoesNotExistException {
        if ((segmentIndex < 0) || (segmentIndex >= _segments.length)) {
            throw new InvalidSegmentIndexException(this, segmentIndex);
        }

        if (_segments[segmentIndex] == null) {
            throw new SegmentDoesNotExistException(this, segmentIndex);
        }

        _segments[segmentIndex] = null;
        _freeSegments.push(segmentIndex);
    }
}
