/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib;

import com.kadware.komodo.baselib.FieldDescriptor;

import java.util.*;

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

    public abstract UndefinedReference copy(
        final FieldDescriptor fieldDescriptor
    );

    @Override
    public abstract boolean equals(final Object obj);

    @Override
    public abstract int hashCode();

    @Override
    public abstract String toString();

    /**
     * Coalesces an array of U'Rs based on collections of field- and id-equivalent U'Rs.
     * It is hoped, but not algorithmically necessary, that no two U'Rs have overlapping but non-equivalent field specs.
     */
    public static UndefinedReference[] coalesce(
        final UndefinedReference[] array
    ) {
        Map<UndefinedReference, Integer> tallyMap = new HashMap<>();
        for (UndefinedReference ur : array) {
            int addend = ur._isNegative ? -1 : 1;
            Integer tally = tallyMap.get(ur);
            if (tally == null) {
                tally = addend;
            } else {
                tally += addend;
            }
            tallyMap.put(ur, tally);
        }

        List<UndefinedReference> resultList = new LinkedList<>();
        for (Map.Entry<UndefinedReference, Integer> entry : tallyMap.entrySet()) {
            if (entry.getValue() != 0) {
                boolean isNegative = entry.getValue() < 0;
                UndefinedReference newUR = entry.getKey().copy(isNegative);
                for (int x = 0; x < Math.abs(entry.getValue()); ++x) {
                    resultList.add(newUR);
                }
            }
        }

        return resultList.toArray(new UndefinedReference[0]);
    }

    /**
     * Tests two arrays of U'Rs, to see if they are equivalent
     */
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
