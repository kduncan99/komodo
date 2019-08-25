/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

import static org.junit.Assert.*;
import org.junit.Test;

import com.kadware.komodo.baselib.TDate;

/**
 * Unit tests for TDate class
 */
public class Test_TDate {

    @Test
    public void ctor(
    ) {
        TDate tdate = new TDate(12, 31, 12, 3600);  //  Dec 31, 1976 at 6:00AM (Happy Birthday Tu)
        assertEquals(0_14_37_14_007020l, tdate.getW());
    }

    @Test
    public void getDay(
    ) {
        TDate tdate = new TDate(12, 31, 12, 3600);
        assertEquals(31, tdate.getDay());
    }

    @Test
    public void getMonth(
    ) {
        TDate tdate = new TDate(12, 31, 12, 3600);
        assertEquals(12, tdate.getMonth());
    }

    @Test
    public void getSeconds(
    ) {
        TDate tdate = new TDate(12, 31, 12, 3600);
        assertEquals(3600, tdate.getSeconds());
    }

    @Test
    public void getYear(
    ) {
        TDate tdate = new TDate(12, 31, 12, 3600);
        assertEquals(12, tdate.getYear());
    }

    @Test
    public void setDay(
    ) {
        TDate tdate = new TDate(0, 0, 0, 0);
        TDate result = tdate.setDay(31);
        assertEquals(0_00_37_00_000000l, result.getW());
    }

    @Test
    public void setMonth(
    ) {
        TDate tdate = new TDate(0, 0, 0, 0);
        TDate result = tdate.setMonth(12);
        assertEquals(0_14_00_00_000000l, result.getW());
    }

    @Test
    public void setSeconds(
    ) {
        TDate tdate = new TDate(0, 0, 0, 0);
        TDate result = tdate.setSeconds(3600);
        assertEquals(0_00_00_00_007020l, result.getW());
    }

    @Test
    public void setYear(
    ) {
        TDate tdate = new TDate(0, 0, 0, 0);
        TDate result = tdate.setYear(12);
        assertEquals(0_00_00_14_000000l, result.getW());
    }

}
