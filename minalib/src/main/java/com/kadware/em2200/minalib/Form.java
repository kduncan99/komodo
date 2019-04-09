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
     * <p>
     * @param fieldSizes
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
     * <p>
     * @param obj
     * <p>
     * @return
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
     * <p>
     * @return
     */
    public int getFieldCount(
    ) {
        return _fieldSizes.length;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int[] getFieldSizes(
    ) {
        return _fieldSizes;
    }

    /**
     * Getter
     * <p.
     * @return
     */
    public int getLeftSlop(
    ) {
        return _leftSlop;
    }
}
