/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.baselib.FieldDescriptor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for all undefined references
 */
public abstract class UndefinedReference {

    public final FieldDescriptor _fieldDescriptor;
    public final boolean _isNegative;

    UndefinedReference(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative
    ) {
        _fieldDescriptor = fieldDescriptor;
        _isNegative = isNegative;
    }

    public abstract UndefinedReference copy(
        final boolean isNegative
    );

    @Override
    public abstract boolean equals(final Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    public static boolean equals(
        final UndefinedReference[] array1,
        final UndefinedReference[] array2
    ) {
        List<UndefinedReference> list1 = Arrays.asList(array1);
        List<UndefinedReference> list2 = Arrays.asList(array2);
        Iterator<UndefinedReference> iter1 = list1.iterator();
        while (iter1.hasNext()) {
            UndefinedReference ref1 = iter1.next();
            Iterator<UndefinedReference> iter2 = list2.iterator();
            while (iter2.hasNext()) {
                UndefinedReference ref2 = iter2.next();
                if (ref1.equals(ref2)) {
                    iter1.remove();
                    iter2.remove();
                    break;
                }
            }
        }

        return list1.isEmpty() && list2.isEmpty();
    }
}
