/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.FieldDescriptor;
import com.kadware.em2200.baselib.Word36;
import java.util.Arrays;
import java.util.List;

/**
 * Contains a value for storage, which might be extended by certain control and/or
 * currently-undefined references.
 */
public class RelocatableWord36 extends Word36 {

    public static class UndefinedReference {

        public final FieldDescriptor _fieldDescriptor;
        public final boolean _isNegative;
        public final String _reference;

        public UndefinedReference(
                final String reference,
                final FieldDescriptor fieldDescriptor,
                final boolean isNegative
        ) {
            _fieldDescriptor = fieldDescriptor;
            _isNegative = isNegative;
            _reference = reference;
        }

        @Override
        public boolean equals(
            final Object obj
        ) {
            if (obj instanceof UndefinedReference) {
                UndefinedReference refObj = (UndefinedReference) obj;
                return (_fieldDescriptor.equals( refObj._fieldDescriptor ))
                        && (_isNegative == refObj._isNegative)
                        && (_reference.equals( refObj._reference ));
            }
            return false;
        }

        @Override
        public String toString(
        ) {
            return String.format("[%d.%d]%s%s",
                                 _fieldDescriptor._startingBit,
                                 _fieldDescriptor._fieldSize,
                                 _isNegative ? "-" : "+",
                                 _reference);
        }
    }

    public final UndefinedReference[] _undefinedReferences;

    /**
     * Constructor
     */
    public RelocatableWord36(
        final long value,
        final UndefinedReference[] undefinedReferences
    ) {
        super(value);
        _undefinedReferences = undefinedReferences == null ? new UndefinedReference[0] : undefinedReferences;
    }

    /**
     * Constructor
     */
    public RelocatableWord36(
        final Word36 value,
        final UndefinedReference[] undefinedReferences
    ) {
        super(value.getW());
        _undefinedReferences = undefinedReferences == null ? new UndefinedReference[0] : undefinedReferences;
    }

    /**
     * Test for equality - works for this object and for Word36
     * @param obj comparison object
     * @return true if the objects are equal, else false
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof Word36) {
            RelocatableWord36 rw36 = (RelocatableWord36) obj;
            return (getW() == rw36.getW()) && equals(_undefinedReferences, rw36._undefinedReferences);
        }

        return false;
    }

    /**
     * Compares two arrays of undefined reference objects to ensure they are semantically equal.
     * They are equal if there is a one-to-one mapping of equal objects between the two arrays.
     * @param array1 first array
     * @param array2 second array
     * @return true if the arrays are equal, else false
     */
    private static boolean equals(
        final UndefinedReference[] array1,
        final UndefinedReference[] array2
    ) {
        List<UndefinedReference> temp = Arrays.asList(array2);
        for (UndefinedReference ref1 : array1) {
            boolean found = false;
            for (UndefinedReference ref2 : temp) {
                if (ref1.equals(ref2)) {
                    temp.remove(ref2);
                    found = true;
                    break;
                }
            }

            if (!found) {
                return false;
            }
        }

        return true;
    }
}
