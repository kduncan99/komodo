/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import java.util.Arrays;

/**
 * Represents a form
 */
public class Form {

    private final int[] _fieldSizes;
    private final int _leftSlop;
    private static final int[] _emptyFormInts = { 0 };
    private static final int[] _fullFormInts = { 72 };

    public static final Form EMPTY_FORM = new Form(_emptyFormInts);
    public static final Form FULL_FORM = new Form(_fullFormInts);

    /**
     * Constructor
     * @param fieldSizes sizes of the various bitfields
     */
    public Form(
        final int[] fieldSizes
    ) {
        _fieldSizes = fieldSizes;
        int total = 0;
        for (int fs : fieldSizes) {
            total += fs;
        }
        _leftSlop = 72 - total;
    }

    /**
     * check for equality
     * @param obj comparison object
     * @return true if objects are equal
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof Form) {
            Form f = (Form)obj;
            return Arrays.equals(_fieldSizes, f._fieldSizes);
        }

        return false;
    }

    /**
     * Getter
     * @return number of field definitions
     */
    public int getFieldCount(
    ) {
        return _fieldSizes.length;
    }

    /**
     * Getter
     * @return array of bit field sizes
     */
    public int[] getFieldSizes(
    ) {
        return _fieldSizes;
    }

    /**
     * Getter
     * @return slop - i.e., bits not accounted for
     */
    public int getLeftSlop(
    ) {
        return _leftSlop;
    }
}
