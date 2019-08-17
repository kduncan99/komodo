/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.OnesComplement;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A Value which represents a 36-bit signed integer. Note that this differs from the way MASM works.
 */
public class IntegerValue extends Value {

    public static class Field {
        private final FieldDescriptor _fieldDescriptor;
        private final long _intrinsicValue;
        private final UndefinedReference[] _undefinedReferences;  //  may be empty, but never null

        public Field(
            final FieldDescriptor fieldDescriptor,
            final long intrinsicValue,
            final UndefinedReference[] undefinedReferences
        ) throws InvalidParameterException {
            _fieldDescriptor = fieldDescriptor;
            _intrinsicValue = intrinsicValue;
            _undefinedReferences = undefinedReferences;

            if ((fieldDescriptor._fieldSize == 0)
                || (fieldDescriptor._startingBit + fieldDescriptor._fieldSize > 36)) {
                throw new InvalidParameterException("Bad field descriptor");
            }

            long limit = 0_777777_777777L << fieldDescriptor._fieldSize;
            if ((intrinsicValue & limit) != 0) {
                throw new InvalidParameterException("Truncation of value in described field");
            }
        }
    }

    //  Fields, in order by the starting bit number of the bit field
    public final Map<Integer, Field> _fields = new TreeMap<>();

    /**
     * general constructor
     * @param flagged - leading asterisk
     * @param fields - fields describing the complete value of this item - must be in MSB->LSB order with no holes
     */
    public IntegerValue(
        final boolean flagged,
        final Field[] fields
    ) throws InvalidParameterException {
        super(flagged);

        int bit = 0;
        for (Field f : fields) {
            if (f._fieldDescriptor._startingBit != bit) {
                throw new InvalidParameterException("Bad form parameters");
            }

            bit += f._fieldDescriptor._fieldSize;
            _fields.put(f._fieldDescriptor._startingBit, f);
        }

        if (bit != 36) {
            throw new InvalidParameterException("Bad form parameters");
        }
    }

    /**
     * constructor for the simplest case
     */
    public IntegerValue(
        final long intrinsicValue
    ) {
        super(false);
        try {
            Field f = new Field(new FieldDescriptor(0, 36),
                                intrinsicValue,
                                new UndefinedReference[0]);
            _fields.put(0, f);
        } catch (InvalidParameterException ex) {
            throw new RuntimeException("Caught " + ex.getMessage());
        }
    }

    /**
     * constructor for the possibly flagged simplest case
     */
    public IntegerValue(
        final boolean flagged,
        final long intrinsicValue
    ) {
        super(flagged);
        try {
            Field f = new Field(new FieldDescriptor(0, 36),
                                intrinsicValue,
                                new UndefinedReference[0]);
            _fields.put(0, f);
        } catch (InvalidParameterException ex) {
            throw new RuntimeException("Caught " + ex.getMessage());
        }
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws FormException if the form attached to the comparison object is not equal to the one attached to this object
     * @throws RelocationException if the relocation info for this object and the comparison object are incompatible
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws FormException,
             RelocationException,
             TypeException {
        if (obj instanceof IntegerValue) {
            IntegerValue iobj = (IntegerValue)obj;
            if ((iobj._flagged == _flagged) && (iobj._fields.size() == _fields.size())) {
                Field[] iobjFields = iobj._fields.values().toArray(new Field[0]);
                Field[] ourFields = _fields.values().toArray(new Field[0]);
                for (int fx = 0; fx < iobjFields.length; ++fx) {
                    if (!iobjFields[fx]._fieldDescriptor.equals(ourFields[fx]._fieldDescriptor)) {
                        throw new FormException("Unequal attached forms");
                    }

                    if ((iobjFields[fx]._undefinedReferences.length != 0) || (ourFields[fx]._undefinedReferences.length != 0)) {
                        throw new RelocationException();
                    }

                    if (ourFields[fx]._intrinsicValue < iobjFields[fx]._intrinsicValue) {
                        return -1;
                    } else if (ourFields[fx]._intrinsicValue > iobjFields[fx]._intrinsicValue) {
                        return 1;
                    }
                }

                return 0;
            }
        }

        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param newFlagged new attribute value
     * @return new value
     */
    @Override
    public Value copy(
        final boolean newFlagged
    ) {
        try {
            return new IntegerValue(newFlagged, _fields.values().toArray(new Field[0]));
        } catch (InvalidParameterException ex) {
            //  Can't happen.
            throw new RuntimeException("Caught " + ex.getMessage());
        }
    }

    /**
     * Check for equality
     * @param obj comparison object
     * @return true if comparison object is equal to this one
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        try {
            return compareTo(obj) == 0;
        } catch (FormException | RelocationException | TypeException ex) {
            return false;
        }
    }

    /**
     * Generate hash code
     */
    @Override
    public int hashCode() {
        int result = 0;
        for (Field f : _fields.values()) {
            result ^= f._intrinsicValue;
        }

        return result;
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.Integer;
    }

    /**
     * Transform the value to a FloatingPointValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new value
     */
    @Override
    public FloatingPointValue toFloatingPointValue(
        final Locale locale,
        Diagnostics diagnostics
    ) {
        if (_fields.size() != 1) {
            diagnostics.append(new ErrorDiagnostic(locale, "Integer value has a form attached"));
            return new FloatingPointValue(false, 0.0);
        }

        Field field = _fields.get(0);
        if (field._undefinedReferences.length == 0) {
            diagnostics.append(new RelocationDiagnostic(locale));
            return new FloatingPointValue(false, 0.0);
        }

        return new FloatingPointValue(false, field._intrinsicValue);
    }

    /**
     * Transform the value to an IntegerValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new value
     */
    @Override
    public IntegerValue toIntegerValue(
        final Locale locale,
        Diagnostics diagnostics
    ) {
        return this;
    }

    /**
     * Transform the value to a StringValue, if possible.
     * This presumes that the integer value is one field with no relocation.
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
     * @param diagnostics where we post any necessary diagnostics
     * @return new value
     */
    @Override
    public StringValue toStringValue(
        final Locale locale,
        final CharacterMode characterMode,
        Diagnostics diagnostics
    ) {
        if (_fields.size() != 1) {
            diagnostics.append(new ErrorDiagnostic(locale, "Integer value has a form attached"));
            return new StringValue(false, "    ", characterMode);
        }

        Field field = _fields.get(0);
        if (field._undefinedReferences.length == 0) {
            diagnostics.append(new RelocationDiagnostic(locale));
            return new StringValue(false, "    ", characterMode);
        }

        if (characterMode == CharacterMode.ASCII) {
            return new StringValue(false,
                                   Word36.toASCII(_fields.get(0)._intrinsicValue),
                                   characterMode);
        } else {
            return new StringValue(false,
                                   Word36.toFieldata(_fields.get(0)._intrinsicValue),
                                   characterMode);
        }
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (_flagged) { sb.append("*"); }

        for (Field f : _fields.values()) {
            sb.append(String.format(" %s%012o", f._fieldDescriptor.toString(), f._intrinsicValue));
            for (UndefinedReference ur : f._undefinedReferences) {
                sb.append(ur.toString());
            }
        }
        return sb.toString();
    }

    /**
     * Add two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param addend1 IntegerValue addend
     * @param addend2 IntegerValue addend
     * @return new IntegerValue object representing the sum of the two addends
     */
    public static IntegerValue add(
        final IntegerValue addend1,
        final IntegerValue addend2,
        final Locale locale,
        final Diagnostics diagnostics
    ) throws FormException,
             TypeException {
        if (addend1._flagged || addend2._flagged) {
            throw new TypeException("Cannot perform arithmetic on flagged values");
        }

        if (addend1._fields.size() != addend2._fields.size()) {
            throw new FormException("Operands have incompatible forms");
        }

        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
        Field[] fields1 = addend1._fields.values().toArray(new Field[0]);
        Field[] fields2 = addend2._fields.values().toArray(new Field[0]);
        Field[] resultFields = new Field[fields1.length];
        int rfx = 0;

        try {
            for (int fx = 0; fx < fields1.length; ++fx) {
                Field f1 = fields1[fx];
                Field f2 = fields2[fx];
                FieldDescriptor fd1 = f1._fieldDescriptor;
                FieldDescriptor fd2 = f2._fieldDescriptor;
                if (!fd1.equals(fd2)) {
                    throw new FormException("Operands have incompatible forms");
                }

                long intrinstic1 = extendSign(f1._intrinsicValue, fd1._fieldSize);
                long intrinstic2 = extendSign(f2._intrinsicValue, fd2._fieldSize);
                OnesComplement.add36(intrinstic1, intrinstic2, ar);
                if (ar._overflow) {
                    diagnostics.append(new TruncationDiagnostic(locale, String.format("Truncation in field %s", fd1)));
                }

                int shift = 36 - (fd1._startingBit + fd1._fieldSize);
                long fieldValue = ar._sum << shift;

                //  normalize the UR's - let any inverses drop out
                List<UndefinedReference> allRefs = Arrays.asList(f1._undefinedReferences);
                allRefs.addAll(Arrays.asList(f2._undefinedReferences));
                List<UndefinedReference> normRefs = normalizeUndefinedReferences(allRefs);

                resultFields[rfx++] = new Field(fd1, fieldValue, normRefs.toArray(new UndefinedReference[0]));
            }

            return new IntegerValue(false, resultFields);
        } catch (InvalidParameterException ex) {
            throw new RuntimeException("Caught " + ex.getMessage());
        }
    }

    /**
     * Performs a sign-extension on an integer of any field size from 1 to 36 bits
     * @param operand original value
     * @param fieldSize size of the field in bits
     * @return sign-extended value
     */
    private static long extendSign(
        final long operand,
        final int fieldSize
    ) {
        if (fieldSize < 36) {
            long checkMask = 01 << (fieldSize - 1);
            if ((operand & checkMask) != 0) {
                long extension = 0_777777_777777L << (fieldSize - 1);
                return (extension | operand) & 0_777777_777777L;
            }
        }

        return operand;
    }

    /**
     * Normalizes a list of undefined references to drop out inverses
     * i.e., we would lose a positive and negative pair of the otherwise-same undefined labels.
     * @param source source list
     * @return normalized list
     */
    private static List<UndefinedReference> normalizeUndefinedReferences(
        final List<UndefinedReference> source
    ) {
        Map<UndefinedReference, Integer> temp = new HashMap<>();
        for (UndefinedReference ur : source) {
            Integer count = temp.get(ur);
            if (count == null) {
                count = 0;
            }

            count = (ur._isNegative ? count - 1 : count + 1);
            temp.put(ur, count);
        }

        List<UndefinedReference> result = new LinkedList<>();
        for (Map.Entry<UndefinedReference, Integer> entry : temp.entrySet()) {
            int count = Math.abs(entry.getValue());
            boolean isNegative = entry.getValue() < 0;
            for (int c = 0; c < count; ++c) {
                result.add(entry.getKey().copy(isNegative));
            }
        }

        return result;
    }
}
