/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

import com.kadware.komodo.baselib.FieldDescriptor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Base class for all undefined references
 */
public abstract class UnresolvedReference {

    public final FieldDescriptor _fieldDescriptor;
    public final boolean _isNegative;

    /**
     * constructor
     * @param fieldDescriptor describes the subset of the containing value, to which this reference applies
     * @param isNegative true if this value is to be arithmetically inverted when it is integrated into the containing value
     */
    UnresolvedReference(
        final FieldDescriptor fieldDescriptor,
        final boolean isNegative
    ) {
        _fieldDescriptor = fieldDescriptor;
        _isNegative = isNegative;
    }

    public abstract UnresolvedReference copy(boolean isNegative);
    public abstract UnresolvedReference copy(FieldDescriptor fieldDescriptor);

    @Override public abstract boolean equals(final Object obj);
    @Override public abstract int hashCode();
    @Override public abstract String toString();

    /**
     * Coalesces an array of U'Rs based on collections of field- and id-equivalent U'Rs.
     * It is hoped, but not algorithmically necessary, that no two U'Rs have overlapping but non-equivalent field specs.
     * We must use a LinkedHashMap so that the references retain their ordering, to the extend possible.
     * This is required for things such as LBDIREF$ to work.
     */
    public static UnresolvedReference[] coalesce(
        final UnresolvedReference[] array
    ) {
        Map<UnresolvedReference, Integer> tallyMap = new LinkedHashMap<>();
        for (UnresolvedReference ur : array) {
            UnresolvedReference urAbs = ur.copy(false);
            int addend = ur._isNegative ? -1 : 1;
            Integer tally = tallyMap.get(urAbs);
            if (tally == null) {
                tally = addend;
            } else {
                tally += addend;
            }
            tallyMap.put(urAbs, tally);
        }

        List<UnresolvedReference> resultList = new LinkedList<>();
        for (Map.Entry<UnresolvedReference, Integer> entry : tallyMap.entrySet()) {
            if (entry.getValue() != 0) {
                boolean isNegative = entry.getValue() < 0;
                UnresolvedReference newUR = entry.getKey().copy(isNegative);
                for (int x = 0; x < Math.abs(entry.getValue()); ++x) {
                    resultList.add(newUR);
                }
            }
        }

        return resultList.toArray(new UnresolvedReference[0]);
    }

    /**
     * Tests two arrays of U'Rs, to see if they are equivalent
     */
    public static boolean equals(
        final UnresolvedReference[] array1,
        final UnresolvedReference[] array2
    ) {
        List<UnresolvedReference> list1 = new LinkedList<>(Arrays.asList(array1));
        List<UnresolvedReference> list2 = new LinkedList<>(Arrays.asList(array2));
        Iterator<UnresolvedReference> iter1 = list1.iterator();

        while (iter1.hasNext()) {
            UnresolvedReference ref1 = iter1.next();
            Iterator<UnresolvedReference> iter2 = list2.iterator();
            while (iter2.hasNext()) {
                UnresolvedReference ref2 = iter2.next();
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
