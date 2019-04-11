/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib;

import com.kadware.em2200.baselib.Word36;

import javax.xml.stream.Location;
import java.util.LinkedList;
import java.util.List;

/**
 * Contains a value for storage, which might be extended by certain control and/or
 * currently-undefined references.
 */
public class RelocatableWord36 extends Word36 {

    public enum PartialWordField {

        H1(0, 18),
        H2(18,18),
        T1(0, 12),
        T2(12,12),
        T3(24,12),
        Q1(0, 9),
        Q2(9, 9),
        Q3(18,9),
        Q4(27,9),
        S1(0, 6),
        S2(6, 6),
        S3(12,6),
        S4(18,6),
        S5(24,6),
        S6(30,6),
        U(20,16);

        private final int _startingBit;
        private final int _fieldSize;

        PartialWordField(
            final int startingBit,
            final int fieldSize
        ) {
            _startingBit = startingBit;
            _fieldSize = fieldSize;
        }

        public FieldDescriptor getFieldDescriptor(
        ) {
            return new FieldDescriptor(_startingBit, _fieldSize);
        }
    }

    public static class FieldDescriptor {
        public final int _startingBit;      //  0 is MSB, 35 is LSB
        public final int _fieldSize;        //  number of bits in the field

        public FieldDescriptor(
            final int startingBit,
            final int fieldSize
        ) {
            _startingBit = startingBit;
            _fieldSize = fieldSize;
        }

        @Override
        public boolean equals(
            final Object obj
        ) {
            if (obj instanceof FieldDescriptor) {
                FieldDescriptor fdObj = (FieldDescriptor) obj;
                if ((fdObj._fieldSize == _fieldSize) && (fdObj._startingBit == _startingBit)) {
                    return true;
                }
            }

            return false;
        }
    }

    public static abstract class Modifier {
        public final FieldDescriptor _fieldDescriptor;

        public Modifier(
            final FieldDescriptor fieldDescriptor
        ) {
            _fieldDescriptor = fieldDescriptor;
        }
    }

    public static class LocationCounterReference extends Modifier {

        public final int _locationCounterIndex;

        public LocationCounterReference(
            final FieldDescriptor fieldDescriptor,
            final int locationCounterIndex
        ) {
            super(fieldDescriptor);
            _locationCounterIndex = locationCounterIndex;
        }

        public boolean equals(
            final Object obj
        ) {
            if (obj instanceof LocationCounterReference) {
                LocationCounterReference lcRef = (LocationCounterReference) obj;
                if ((_fieldDescriptor.equals(lcRef._fieldDescriptor))
                    && (_locationCounterIndex == lcRef._locationCounterIndex)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class ExternalReference extends Modifier {
        public final String _reference;

        public ExternalReference(
            final FieldDescriptor fieldDescriptor,
            final String reference
        ) {
            super(fieldDescriptor);
            _reference = reference;
        }

        public boolean equals(
            final Object obj
        ) {
            if (obj instanceof ExternalReference) {
                ExternalReference lcRef = (ExternalReference) obj;
                if ((_fieldDescriptor.equals(lcRef._fieldDescriptor))
                    && (_reference == lcRef._reference)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final List<Modifier> _modifiers = new LinkedList<>();


    /**
     * Constructor
     */
    public RelocatableWord36(
    ) {
        super();
    }

    /**
     * Constructor
     */
    public RelocatableWord36(
        final long value
    ) {
        super(value);
    }

    /**
     * Constructor
     */
    public RelocatableWord36(
        final long value,
        final List<Modifier> modifiers
    ) {
        super(value);
        _modifiers.addAll(modifiers);
    }

    /**
     * Constructor
     */
    public RelocatableWord36(
        final Word36 value
    ) {
        super(value.getW());
    }

    /**
     * Constructor
     */
    public RelocatableWord36(
        final Word36 value,
        final List<Modifier> modifiers
    ) {
        super(value.getW());
        _modifiers.addAll(modifiers);
    }

    /**
     * Test for equality - works for this object and for Word36
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof RelocatableWord36) {
            RelocatableWord36 rw = (RelocatableWord36)obj;
            return (getW() == rw.getW()) && (equals(_modifiers, rw._modifiers));
        } else if (obj instanceof Word36) {
            return _modifiers.isEmpty() && (getW() == ((Word36)obj).getW());
        }
        return false;
    }

    public static boolean equals(
        List<Modifier> list1,
        List<Modifier> list2
    ) {
        List<Modifier> temp = new LinkedList<>();
        temp.addAll(list2);
        for (Modifier m1 : list1) {
            boolean found = false;
            for (Modifier m2 : temp) {
                if (m1.equals(m2)) {
                    temp.remove(m2);
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
