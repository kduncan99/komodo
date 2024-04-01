/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * A Word36 object overloaded to additionally describe a time and date in TDATE format
 */

public class TDate extends Word36 {

    /**
     * Constructor
     * @param month 1=Jan, 2=Feb, etc
     * @param day Day of month, from 1 to 31
     * @param year modulo 1964 (this only works up until 2027... which is a problem for us)
     * @param seconds since midnight
     */
    public TDate(
        final long month,
        final long day,
        final long year,
        final long seconds
    ) {
        super(((month & 077) << 30) | ((day & 077) << 24) | ((year & 077) << 18) | (seconds & 0777777));
    }

    /**
     * Another constructor
     */
    public TDate(Word36 value) { super(value); }

    /**
     * Get calendar day of the month
     */
    public long getDay() { return getS2(_value); }

    /**
     * Get month of the year, from 1 to 23
     */
    public long getMonth() { return getS1(_value); }

    /**
     * Get seconds since midnight
     */
    public long getSeconds() { return getH2(_value); }

    /**
     * Get year, modulo 1964
     */
    public long getYear() { return getS3(_value); }

    /**
     * Setter
     * @param value day of the month, from 1 to 31
     */
    public TDate setDay(long value) { return new TDate(setS2(value)); }

    /**
     * Setter
     * @param value month, from 1 to 12
     */
    public TDate setMonth(long value) { return new TDate(setS1(value)); }

    /**
     * Setter
     * @param value seconds since midnight
     */
    public TDate setSeconds(long value) { return new TDate(setH2(value)); }

    /**
     * Setter
     * @param value year, modulo 1964
     */
    public TDate setYear(long value) { return new TDate(setS3(value)); }
}
