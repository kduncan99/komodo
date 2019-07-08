/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import java.util.List;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Unit tests for RegionContainer class
 */
public class Test_RegionContainer {

    @Test
    public void creation_1(
    ) {
        RegionContainer c = new RegionContainer();
        assertTrue(c.getRegions().isEmpty());
        assertTrue(c.isEmpty());
    }

    @Test
    public void creation_2(
    ) {
        RegionContainer c = new RegionContainer(new Identifier(5), new Counter(10));
        List<Region> regions = c.getRegions();
        assertEquals(1, regions.size());
        assertEquals(5, regions.get(0).getFirstUnit().getValue());
        assertEquals(10, regions.get(0).getExtent().getValue());
    }

    @Test
    public void creation_3(
    ) {
        Region r = new Region(5, 10);
        RegionContainer c = new RegionContainer(r);
        List<Region> regions = c.getRegions();
        assertEquals(1, regions.size());
        assertEquals(5, regions.get(0).getFirstUnit().getValue());
        assertEquals(10, regions.get(0).getExtent().getValue());
    }

    @Test
    public void creation_4(
    ) {
        RegionContainer c = new RegionContainer(5, 10);
        List<Region> regions = c.getRegions();
        assertEquals(1, regions.size());
        assertEquals(5, regions.get(0).getFirstUnit().getValue());
        assertEquals(10, regions.get(0).getExtent().getValue());
    }

    @Test
    public void isEmpty_False(
    ) {
        RegionContainer c = new RegionContainer(0, 1);
        assertFalse(c.isEmpty());
    }

    @Test
    public void isEmpty_True_1(
    ) {
        RegionContainer c = new RegionContainer();
        assertTrue(c.isEmpty());
    }

    @Test
    public void isEmpty_True_2(
    ) {
        RegionContainer c = new RegionContainer();
        c.append(new Region());
        c.append(new Region());
        assertTrue(c.isEmpty());
    }

    @Test
    public void append_Normal(
    ) {
        RegionContainer c = new RegionContainer();
        assertTrue(c.append(new Region(10, 5)));
        assertTrue(c.append(new Region(20, 5)));
        List<Region> regions = c.getRegions();
        assertEquals(2, regions.size());
        assertEquals(10, regions.get(0).getFirstUnit().getValue());
        assertEquals(20, regions.get(1).getFirstUnit().getValue());
    }

    @Test
    public void append_Rejected_1(
    ) {
        RegionContainer c = new RegionContainer(10, 5);
        assertFalse(c.append(new Region(10, 5)));
    }

    @Test
    public void append_Rejected_2(
    ) {
        RegionContainer c = new RegionContainer(10, 5);
        assertFalse(c.append(new Region(5, 6)));
    }

    @Test
    public void append_Rejected_3(
    ) {
        RegionContainer c = new RegionContainer(10, 5);
        assertFalse(c.append(new Region(14, 6)));
    }

    @Test
    public void carve_False_1(
    ) {
        RegionContainer c = new RegionContainer();
        assertFalse(c.carve(new Region(5, 10)));
    }

    @Test
    public void carve_False_2(
    ) {
        RegionContainer c = new RegionContainer(10, 10);
        assertFalse(c.carve(new Region(0, 10)));
    }

    @Test
    public void carve_False_3(
    ) {
        RegionContainer c = new RegionContainer(10, 10);
        assertFalse(c.carve(new Region(20,5)));
    }

    @Test
    public void carve_False_4(
    ) {
        RegionContainer c = new RegionContainer();
        c.append(new Region(10, 10));
        c.append(new Region(30, 10));
        assertFalse(c.carve(new Region(20, 10)));
    }

    @Test
    public void carve_Exact(
    ) {
        RegionContainer c = new RegionContainer(10, 5);
        assertTrue(c.carve(new Region(10, 5)));
        assertTrue(c.isEmpty());
    }

    @Test
    public void carve_LeftOverlap(
    ) {
        RegionContainer c = new RegionContainer(10, 10);
        assertTrue(c.carve(new Region(5, 10)));

        List<Region> regions = c.getRegions();
        assertEquals(1, regions.size());

        Region r1 = regions.get(0);
        assertEquals(15, r1.getFirstUnit().getValue());
        assertEquals(5, r1.getExtent().getValue());
    }

    @Test
    public void carve_LeftSubset(
    ) {
        RegionContainer c = new RegionContainer(10, 10);
        assertTrue(c.carve(new Region(10, 5)));

        List<Region> regions = c.getRegions();
        assertEquals(1, regions.size());

        Region r1 = regions.get(0);
        assertEquals(15, r1.getFirstUnit().getValue());
        assertEquals(5, r1.getExtent().getValue());
    }

    @Test
    public void carve_InternalSubset(
    ) {
        RegionContainer c = new RegionContainer(10, 10);
        assertTrue(c.carve(new Region(11, 8)));

        List<Region> regions = c.getRegions();
        assertEquals(2, regions.size());

        Region r1 = regions.get(0);
        Region r2 = regions.get(1);
        assertEquals(10, r1.getFirstUnit().getValue());
        assertEquals(1, r1.getExtent().getValue());
        assertEquals(19, r2.getFirstUnit().getValue());
        assertEquals(1, r2.getExtent().getValue());
    }

    @Test
    public void carve_RightOverlap(
    ) {
        RegionContainer c = new RegionContainer(10, 10);
        assertTrue(c.carve(new Region(15, 10)));

        List<Region> regions = c.getRegions();
        assertEquals(1, regions.size());

        Region r1 = regions.get(0);
        assertEquals(10, r1.getFirstUnit().getValue());
        assertEquals(5, r1.getExtent().getValue());
    }

    @Test
    public void carve_RightSubset(
    ) {
        RegionContainer c = new RegionContainer(10, 10);
        assertTrue(c.carve(new Region(15, 5)));

        List<Region> regions = c.getRegions();
        assertEquals(1, regions.size());

        Region r1 = regions.get(0);
        assertEquals(10, r1.getFirstUnit().getValue());
        assertEquals(5, r1.getExtent().getValue());
    }

    @Test
    public void carve_ComplexOverlap(
    ) {
        RegionContainer c = new RegionContainer();
        c.append(new Region(10, 10));
        c.append(new Region(30, 10));
        assertTrue(c.carve(new Region(15, 20)));

        List<Region> regions = c.getRegions();
        assertEquals(2, regions.size());
        Region r1 = regions.get(0);
        Region r2 = regions.get(1);
        assertEquals(10, r1.getFirstUnit().getValue());
        assertEquals(5, r1.getExtent().getValue());
        assertEquals(35, r2.getFirstUnit().getValue());
        assertEquals(5, r2.getExtent().getValue());
    }

    @Test
    public void intersection_Complex(
    ) {
        RegionContainer c = new RegionContainer();
        c.append(new Region(10, 10));
        c.append(new Region(30, 10));

        RegionContainer inter = c.intersection(new Region(15, 20));
        List<Region> regions = inter.getRegions();
        assertEquals(2, regions.size());

        Region r1 = regions.get(0);
        Region r2 = regions.get(1);
        assertEquals(15, r1.getFirstUnit().getValue());
        assertEquals(5, r1.getExtent().getValue());
        assertEquals(30, r2.getFirstUnit().getValue());
        assertEquals(5, r2.getExtent().getValue());
    }

    @Test
    public void intersects_Complex(
    ) {
        RegionContainer c = new RegionContainer();
        c.append(new Region(10, 10));
        c.append(new Region(30, 10));

        assertTrue(c.intersects(new Region(15, 20)));
    }

    @Test
    public void intersects_False(
    ) {
        RegionContainer c = new RegionContainer();
        c.append(new Region(10, 10));
        c.append(new Region(30, 10));
        c.append(new Region(50, 10));

        assertFalse(c.intersects(new Region(0, 5)));
        assertFalse(c.intersects(new Region(25, 5)));
        assertFalse(c.intersects(new Region(45, 5)));
        assertFalse(c.intersects(new Region(65, 5)));
    }
}
