/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import java.util.Arrays;

/**
 * Represents a form
 */
public class Form {

    public final int[] _fieldSizes;
    public final int _leftSlop;

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

        if (total > 36) {
            throw new RuntimeException("Bad form instantiation - too many bits");
        }

        _leftSlop = 36 - total;
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
     * Produce human-readable version of this object
     * @return string
     */
    public String toString(
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int ix = 0; ix < _fieldSizes.length; ++ix) {
            sb.append(String.format("%s%d",
                                    ix == 0 ? "" : ",",
                                    _fieldSizes[ix]));
        }
        sb.append(")");
        return sb.toString();
    }
}
