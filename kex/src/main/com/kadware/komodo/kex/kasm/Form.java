/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.FieldDescriptor;
import java.util.Arrays;

/**
 * Represents a form
 */
public class Form {

    private static final int[] _iForm = { 6, 4, 4, 4, 2, 16 };
    private static final int[] _eiForm = { 6, 4, 4, 4, 2, 4, 12 };
    private static final int[] _fjaxhiuForm = { 6, 4, 4, 4, 1, 1, 16 };
    private static final int[] _fjaxuForm = { 6, 4, 4, 4, 18 };
    private static final int[] _fjaxhibdForm = { 6, 4, 4, 4, 1, 1, 4, 12 };

    public static Form I$Form = new Form(_iForm);
    public static Form EI$Form = new Form(_eiForm);
    public static Form FJAXHIU$Form = new Form(_fjaxhiuForm);
    public static Form FJAXU$Form = new Form(_fjaxuForm);
    public static Form FJAXHIBD$Form = new Form(_fjaxhibdForm);

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

    /**
     * Generate an array of field descriptors to represent this form
     */
    public FieldDescriptor[] getFieldDescriptors() {
        FieldDescriptor[] result = new FieldDescriptor[_fieldSizes.length];
        int bit = _leftSlop;
        for (int fx = 0; fx < _fieldSizes.length; ++fx) {
            result[fx] = new FieldDescriptor(bit, _fieldSizes[fx]);
            bit += _fieldSizes[fx];
        }

        return result;
    }
}
