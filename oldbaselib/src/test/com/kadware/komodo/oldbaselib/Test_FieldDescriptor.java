/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.oldbaselib;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for FieldDescriptor class
 */
public class Test_FieldDescriptor {

    private static final Random _random = new Random(System.currentTimeMillis());

    @Test
    public void constructor_ok1() {
        FieldDescriptor fd = new FieldDescriptor(10, 15);
        assertEquals(10, fd._startingBit);
        assertEquals(15, fd._fieldSize);
    }

    @Test
    public void constructor_ok2() {
        FieldDescriptor fd = new FieldDescriptor(0, 1);
        assertEquals(0, fd._startingBit);
        assertEquals(1, fd._fieldSize);
    }

    @Test
    public void constructor_ok3() {
        FieldDescriptor fd = new FieldDescriptor(0, 36);
        assertEquals(0, fd._startingBit);
        assertEquals(36, fd._fieldSize);
    }

    @Test
    public void constructor_ok4() {
        FieldDescriptor fd = new FieldDescriptor(35, 1);
        assertEquals(35, fd._startingBit);
        assertEquals(1, fd._fieldSize);
    }

    @Test(expected = RuntimeException.class)
    public void constructor_fail1() {
        FieldDescriptor fd = new FieldDescriptor(-4, 18);
    }

    @Test(expected = RuntimeException.class)
    public void constructor_fail2() {
        FieldDescriptor fd = new FieldDescriptor(36, 18);
    }

    @Test(expected = RuntimeException.class)
    public void constructor_fail3() {
        FieldDescriptor fd = new FieldDescriptor(0, 0);
    }

    @Test(expected = RuntimeException.class)
    public void constructor_fail4() {
        FieldDescriptor fd = new FieldDescriptor(0, 37);
    }

    @Test(expected = RuntimeException.class)
    public void constructor_fail5() {
        FieldDescriptor fd = new FieldDescriptor(18, 36);
    }

    @Test
    public void equals_true() {
        FieldDescriptor fd = new FieldDescriptor(12, 12);
        assertTrue(fd.equals(fd));
    }

    @Test
    public void equals_false1() {
        FieldDescriptor fdThis = new FieldDescriptor(0, 12);
        FieldDescriptor fdThat = new FieldDescriptor(0, 10);
        assertFalse(fdThis.equals(fdThat));
    }

    @Test
    public void equals_false2() {
        FieldDescriptor fdThis = new FieldDescriptor(0, 12);
        FieldDescriptor fdThat = new FieldDescriptor(4, 12);
        assertFalse(fdThis.equals(fdThat));
    }
}
