/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.processors;

import com.bearsnake.komodo.hardwarelib.exceptions.InvalidSegmentIndexException;
import com.bearsnake.komodo.hardwarelib.exceptions.NoFreeSegmentsException;
import com.bearsnake.komodo.hardwarelib.exceptions.SegmentDoesNotExistException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestMainStorageProcessor {

    private static final String NAME = "MS0";
    private static final String UPI = "12345";
    private static final int SEGMENT_COUNT = 4;
    private MainStorageProcessor processor;

    @BeforeEach
    public void setup() {
        processor = new MainStorageProcessor(NAME, UPI, SEGMENT_COUNT);
    }

    @Test
    public void testConstructor() {
        assertEquals(NAME, processor.getName());
        assertEquals(UPI, processor.getUpi());
        assertTrue(processor.getAllocatedSegmentIndexes().isEmpty());
    }

    @Test
    public void testAllocateSegmentSuccess() throws NoFreeSegmentsException {
        int index = processor.allocateSegment(100);
        assertTrue(index >= 0 && index < SEGMENT_COUNT);
        assertNotNull(processor.getAllocatedSegmentIndexes());
        assertTrue(processor.getAllocatedSegmentIndexes().contains(index));
        assertEquals(1, processor.getAllocatedSegmentIndexes().size());
    }

    @Test
    public void testAllocateMultipleSegments() throws NoFreeSegmentsException {
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            processor.allocateSegment(100);
        }
        assertEquals(SEGMENT_COUNT, processor.getAllocatedSegmentIndexes().size());
    }

    @Test
    public void testAllocateSegmentNoFreeSegments() throws NoFreeSegmentsException {
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            processor.allocateSegment(100);
        }
        assertThrows(NoFreeSegmentsException.class, () -> processor.allocateSegment(100));
    }

    @Test
    public void testClear() throws NoFreeSegmentsException {
        processor.allocateSegment(100);
        processor.allocateSegment(100);
        assertFalse(processor.getAllocatedSegmentIndexes().isEmpty());

        processor.clear();
        assertTrue(processor.getAllocatedSegmentIndexes().isEmpty());
        // Verify we can allocate again up to SEGMENT_COUNT
        for (int i = 0; i < SEGMENT_COUNT; i++) {
            processor.allocateSegment(100);
        }
        assertEquals(SEGMENT_COUNT, processor.getAllocatedSegmentIndexes().size());
    }

    @Test
    public void testGetAllocatedSegmentIndexes() throws NoFreeSegmentsException {
        int i1 = processor.allocateSegment(10);
        int i2 = processor.allocateSegment(20);
        var indexes = processor.getAllocatedSegmentIndexes();
        assertEquals(2, indexes.size());
        assertTrue(indexes.contains(i1));
        assertTrue(indexes.contains(i2));
    }

    @Test
    public void testGetSegmentSuccess() throws NoFreeSegmentsException, InvalidSegmentIndexException, SegmentDoesNotExistException {
        int size = 100;
        int index = processor.allocateSegment(size);
        var segment = processor.getSegment(index);
        assertNotNull(segment);
        assertEquals(size, segment.getSize());
    }

    @Test
    public void testGetSegmentInvalidIndex() {
        assertThrows(InvalidSegmentIndexException.class, () -> processor.getSegment(-1));
        assertThrows(InvalidSegmentIndexException.class, () -> processor.getSegment(SEGMENT_COUNT));
    }

    @Test
    public void testGetSegmentDoesNotExist() {
        assertThrows(SegmentDoesNotExistException.class, () -> processor.getSegment(0));
    }

    @Test
    public void testReleaseSegmentSuccess() throws NoFreeSegmentsException, InvalidSegmentIndexException, SegmentDoesNotExistException {
        int index = processor.allocateSegment(100);
        assertTrue(processor.getAllocatedSegmentIndexes().contains(index));

        processor.releaseSegment(index);
        assertFalse(processor.getAllocatedSegmentIndexes().contains(index));
        assertThrows(SegmentDoesNotExistException.class, () -> processor.getSegment(index));

        // Verify we can re-allocate it
        int newIndex = processor.allocateSegment(50);
        assertEquals(index, newIndex);
    }

    @Test
    public void testReleaseSegmentInvalidIndex() {
        assertThrows(InvalidSegmentIndexException.class, () -> processor.releaseSegment(-1));
        assertThrows(InvalidSegmentIndexException.class, () -> processor.releaseSegment(SEGMENT_COUNT));
    }

    @Test
    public void testReleaseSegmentDoesNotExist() {
        assertThrows(SegmentDoesNotExistException.class, () -> processor.releaseSegment(0));
    }
}
