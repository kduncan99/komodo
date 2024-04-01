/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for Region class
 */
public class Test_Region {

    @Test
    public void creation_1(
    ) {
        Region r = new Region();
        assertEquals(0, r.getFirstUnit());
        assertEquals(0, r.getExtent());
    }

    @Test
    public void creation_2(
    ) {
        Region r = new Region(5, 10);
        assertEquals(5, r.getFirstUnit());
        assertEquals(10, r.getExtent());
    }

    @Test
    public void creation_3(
    ) {
        Region r = new Region(5, 10);
        assertEquals(5, r.getFirstUnit());
        assertEquals(10, r.getExtent());
    }

    @Test
    public void setExtent(
    ) {
        Region r = new Region();
        r.setExtent(25);
        assertEquals(25, r.getExtent());
        assertEquals(0, r.getFirstUnit());
    }

    @Test
    public void setFirstUnit(
    ) {
        Region r = new Region();
        r.setFirstUnit(25);
        assertEquals(0, r.getExtent());
        assertEquals(25, r.getFirstUnit());
    }

    @Test
    public void intersection_Not_1(
    ) {
        Region r1 = new Region(0, 10);
        Region r2 = new Region(10, 10);
        assertTrue(r1.intersection(r2).isEmpty());
    }

    @Test
    public void intersection_Not_2(
    ) {
        Region r1 = new Region(10, 10);
        Region r2 = new Region(0, 10);
        assertTrue(r1.intersection(r2).isEmpty());
    }

    @Test
    public void intersection_Exact(
    ) {
        Region r = new Region(25, 25);
        Region i = r.intersection(r);
        assertEquals(i.getFirstUnit(), r.getFirstUnit());
        assertEquals(i.getExtent(), r.getExtent());
    }

    @Test
    public void intersection_Subset(
    ) {
        Region r1 = new Region(10, 10);
        Region r2 = new Region(12, 5);
        Region i = r1.intersection(r2);
        assertEquals(i.getFirstUnit(), r2.getFirstUnit());
        assertEquals(i.getExtent(), r2.getExtent());
    }

    @Test
    public void intersection_Superset(
    ) {
        Region r1 = new Region(10, 10);
        Region r2 = new Region(5, 50);
        Region i = r1.intersection(r2);
        assertEquals(i.getFirstUnit(), r1.getFirstUnit());
        assertEquals(i.getExtent(), r1.getExtent());
    }

    @Test
    public void intersection_Left(
    ) {
        Region r1 = new Region(10, 10);
        Region r2 = new Region(8, 5);
        Region i = r1.intersection(r2);
        assertEquals(10, i.getFirstUnit());
        assertEquals(3, i.getExtent());
    }

    @Test
    public void intersection_Right(
    ) {
        Region r1 = new Region(10, 10);
        Region r2 = new Region(15, 10);
        Region i = r1.intersection(r2);
        assertEquals(15, i.getFirstUnit());
        assertEquals(5, i.getExtent());
    }

    @Test
    public void intersects_Not_1(
    ) {
        Region main = new Region(10, 10);
        Region comp = new Region(0, 10);
        assertFalse(main.intersects(comp));
    }

    @Test
    public void intersects_Not_2(
    ) {
        Region main = new Region(10, 10);
        Region comp = new Region(20, 5);
        assertFalse(main.intersects(comp));
    }

    @Test
    public void intersects_Exact(
    ) {
        Region main = new Region(10, 10);
        assertTrue(main.intersects(main));
    }

    @Test
    public void intersects_Left(
    ) {
        Region main = new Region(10, 10);
        Region comp = new Region(6, 5);
        assertTrue(main.intersects(comp));
    }

    @Test
    public void intersects_Subset(
    ) {
        Region main = new Region(10, 10);
        Region comp = new Region(12, 5);
        assertTrue(main.intersects(comp));
    }

    @Test
    public void intersects_Right(
    ) {
        Region main = new Region(10, 10);
        Region comp = new Region(19, 5);
        assertTrue(main.intersects(comp));
    }

    @Test
    public void isEmpty_False(
    ) {
        assertFalse((new Region(10, 10)).isEmpty());
    }

    @Test
    public void isEmpty_True(
    ) {
        assertTrue((new Region()).isEmpty());
    }

}
