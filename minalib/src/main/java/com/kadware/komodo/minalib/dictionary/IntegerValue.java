/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.util.Arrays;

/**
 * A Value which represents a 36-bit signed integer. Note that this differs from the way MASM works.
 * Unlike other *Value objects, this one can have a form and multiple undefined reference objects attached.
 */
@SuppressWarnings("Duplicates")
public class IntegerValue extends Value {

    //TODO following is obsolete
//    private static class AdjustedFieldMapsResult {
//        final Map<Integer, Field> _effectiveMap1 = new TreeMap<>();
//        final Map<Integer, Field> _effectiveMap2 = new TreeMap<>();
//        boolean _hasUnequalForms;           //  if true, then the effective maps are different that the input values' maps
//        boolean _lostUndefinedReferences;   //  if true, then the above is true *and* there were undef ref's which were lost
//    }
//
//    public static class DivisionResult {
//        public IntegerValue _quotient;
//        public IntegerValue _remainder;
//        public IntegerValue _coveredQuotient;
//    }
//
//    public static class Field {
//        public final FieldDescriptor _fieldDescriptor;
//        public final long _intrinsicValue;
//        public final UndefinedReference[] _undefinedReferences;  //  may be empty, but never null
//
//        public Field(
//            final FieldDescriptor fieldDescriptor,
//            final long intrinsicValue,
//            final UndefinedReference[] undefinedReferences
//        ) throws InvalidParameterException {
//            _fieldDescriptor = fieldDescriptor;
//            _intrinsicValue = intrinsicValue;
//            _undefinedReferences = undefinedReferences;
//
//            if ((fieldDescriptor._fieldSize == 0)
//                || (fieldDescriptor._startingBit + fieldDescriptor._fieldSize > 36)) {
//                throw new InvalidParameterException("Bad field descriptor");
//            }
//
//            long limit = 0_777777_777777L << fieldDescriptor._fieldSize;
//            if ((intrinsicValue & limit) != 0) {
//                throw new InvalidParameterException("Truncation of value in described field");
//            }
//        }
//
//        /**
//         * Generate additive inverse of this field
//         */
//        Field negate() {
//            UndefinedReference[] newURs = new UndefinedReference[_undefinedReferences.length];
//            for (int ux = 0; ux < newURs.length; ++ux) {
//                newURs[ux] = _undefinedReferences[ux].copy(!_undefinedReferences[ux]._isNegative);
//            }
//
//            try {
//                return new Field(_fieldDescriptor, -_intrinsicValue, newURs);
//            } catch (InvalidParameterException ex) {
//                throw new RuntimeException("Caught " + ex.getMessage());
//            }
//        }
//    }
//
//    //  Fields, in order by the starting bit number of the bit field
//    public final Map<Integer, Field> _fields = new TreeMap<>();
//
//    /**
//     * general constructor
//     * @param flagged - leading asterisk
//     * @param fields - fields describing the complete value of this item - must be in MSB->LSB order with no holes
//     */
//    public IntegerValue(
//        final boolean flagged,
//        final Field[] fields
//    ) throws InvalidParameterException {
//        super(flagged);
//
//        int bit = 0;
//        for (Field f : fields) {
//            if (f._fieldDescriptor._startingBit != bit) {
//                throw new InvalidParameterException("Bad form parameters");
//            }
//
//            bit += f._fieldDescriptor._fieldSize;
//            _fields.put(f._fieldDescriptor._startingBit, f);
//        }
//
//        if (bit != 36) {
//            throw new InvalidParameterException("Bad form parameters");
//        }
//    }
//
//    /**
//     * constructor for the simplest case
//     */
//    public IntegerValue(
//        final long intrinsicValue
//    ) {
//        super(false);
//        try {
//            Field f = new Field(new FieldDescriptor(0, 36),
//                                intrinsicValue,
//                                new UndefinedReference[0]);
//            _fields.put(0, f);
//        } catch (InvalidParameterException ex) {
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    /**
//     * constructor for the possibly flagged simplest case
//     */
//    public IntegerValue(
//        final boolean flagged,
//        final long intrinsicValue
//    ) {
//        super(flagged);
//        try {
//            Field f = new Field(new FieldDescriptor(0, 36),
//                                intrinsicValue,
//                                new UndefinedReference[0]);
//            _fields.put(0, f);
//        } catch (InvalidParameterException ex) {
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    /**
//     * Compares an object to this object
//     * @param obj comparison object
//     * @return -1 if this object sorts before (is less than) the given object
//     *         +1 if this object sorts after (is greater than) the given object,
//     *          0 if both objects sort to the same position (are equal)
//     * @throws FormException if the form attached to the comparison object is not equal to the one attached to this object
//     * @throws RelocationException if the relocation info for this object and the comparison object are incompatible
//     * @throws TypeException if there is no reasonable way to compare the objects
//     */
//    @Override
//    public int compareTo(
//        final Object obj
//    ) throws FormException,
//             RelocationException,
//             TypeException {
//        if (obj instanceof IntegerValue) {
//            IntegerValue iobj = (IntegerValue)obj;
//            if ((iobj._flagged == _flagged) && (iobj._fields.size() == _fields.size())) {
//                Field[] iobjFields = iobj._fields.values().toArray(new Field[0]);
//                Field[] ourFields = _fields.values().toArray(new Field[0]);
//                for (int fx = 0; fx < iobjFields.length; ++fx) {
//                    if (!iobjFields[fx]._fieldDescriptor.equals(ourFields[fx]._fieldDescriptor)) {
//                        throw new FormException();
//                    }
//
//                    if ((iobjFields[fx]._undefinedReferences.length != 0) || (ourFields[fx]._undefinedReferences.length != 0)) {
//                        throw new RelocationException();
//                    }
//
//                    if (ourFields[fx]._intrinsicValue < iobjFields[fx]._intrinsicValue) {
//                        return -1;
//                    } else if (ourFields[fx]._intrinsicValue > iobjFields[fx]._intrinsicValue) {
//                        return 1;
//                    }
//                }
//
//                return 0;
//            }
//        }
//
//        throw new TypeException();
//    }
//
//    /**
//     * Create a new copy of this object, with the given flagged value
//     * @param newFlagged new attribute value
//     * @return new value
//     */
//    @Override
//    public Value copy(
//        final boolean newFlagged
//    ) {
//        try {
//            return new IntegerValue(newFlagged, _fields.values().toArray(new Field[0]));
//        } catch (InvalidParameterException ex) {
//            //  Can't happen.
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    /**
//     * Check for equality
//     * @param obj comparison object
//     * @return true if comparison object is equal to this one
//     */
//    @Override
//    public boolean equals(
//        final Object obj
//    ) {
//        try {
//            return compareTo(obj) == 0;
//        } catch (FormException | RelocationException | TypeException ex) {
//            return false;
//        }
//    }
//
//    /**
//     * Generate hash code
//     */
//    @Override
//    public int hashCode() {
//        int result = 0;
//        for (Field f : _fields.values()) {
//            result ^= f._intrinsicValue;
//        }
//
//        return result;
//    }
//
//    /**
//     * Getter
//     * @return value
//     */
//    @Override
//    public ValueType getType(
//    ) {
//        return ValueType.Integer;
//    }
//
//    /**
//     * Transform the value to a FloatingPointValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new value
//     */
//    @Override
//    public FloatingPointValue toFloatingPointValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) {
//        if (_fields.size() != 1) {
//            diagnostics.append(new ErrorDiagnostic(locale, "Integer value has a form attached"));
//            return new FloatingPointValue(false, 0.0);
//        }
//
//        Field field = _fields.get(0);
//        if (field._undefinedReferences.length == 0) {
//            diagnostics.append(new RelocationDiagnostic(locale));
//            return new FloatingPointValue(false, 0.0);
//        }
//
//        return new FloatingPointValue(false, field._intrinsicValue);
//    }
//
//    /**
//     * Transform the value to an IntegerValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new value
//     */
//    @Override
//    public IntegerValue toIntegerValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) {
//        return this;
//    }
//
//    /**
//     * Transform the value to a StringValue, if possible.
//     * This presumes that the integer value is one field with no relocation.
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param characterMode desired character mode
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new value
//     */
//    @Override
//    public StringValue toStringValue(
//        final Locale locale,
//        final CharacterMode characterMode,
//        Diagnostics diagnostics
//    ) {
//        if (_fields.size() != 1) {
//            diagnostics.append(new ErrorDiagnostic(locale, "Integer value has a form attached"));
//            return new StringValue(false, "    ", characterMode);
//        }
//
//        Field field = _fields.get(0);
//        if (field._undefinedReferences.length == 0) {
//            diagnostics.append(new RelocationDiagnostic(locale));
//            return new StringValue(false, "    ", characterMode);
//        }
//
//        if (characterMode == CharacterMode.ASCII) {
//            return new StringValue(false,
//                                   Word36.toASCII(_fields.get(0)._intrinsicValue),
//                                   characterMode);
//        } else {
//            return new StringValue(false,
//                                   Word36.toFieldata(_fields.get(0)._intrinsicValue),
//                                   characterMode);
//        }
//    }
//
//    /**
//     * For display purposes
//     * @return displayable string
//     */
//    @Override
//    public String toString() {
//        StringBuilder sb = new StringBuilder();
//        if (_flagged) { sb.append("*"); }
//
//        for (Field f : _fields.values()) {
//            sb.append(String.format(" %s%012o", f._fieldDescriptor.toString(), f._intrinsicValue));
//            for (UndefinedReference ur : f._undefinedReferences) {
//                sb.append(ur.toString());
//            }
//        }
//        return sb.toString();
//    }
//
//    //  ----------------------------------------------------------------------------------------------------------------------------
//    //  Special arithmetic and other stuff
//    //  ----------------------------------------------------------------------------------------------------------------------------
//
//    /**
//     * Add two IntegerValue objects, observing attached forms and relocation information (if any)
//     * @param operand1 left-hand operand
//     * @param operand2 right-hand operand
//     * @param locale location of source code in case we need to raise a diagnostic
//     * @param diagnostics container of diagnostics in case we need to raise on
//     * @return new IntegerValue object representing the sum of the two addends
//     */
//    public static IntegerValue add(
//        final IntegerValue operand1,
//        final IntegerValue operand2,
//        final Locale locale,
//        final Diagnostics diagnostics
//    ) {
//        AdjustedFieldMapsResult afmResult = adjustFieldMaps(operand1._fields, operand2._fields);
//        if (afmResult._hasUnequalForms) {
//            diagnostics.append(new FormDiagnostic(locale));
//            if (afmResult._lostUndefinedReferences) {
//                diagnostics.append(new RelocationDiagnostic(locale));
//            }
//        }
//
//        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
//        Field[] resultFields = new Field[operand1._fields.size()];
//        int rfx = 0;
//        try {
//            for (Map.Entry<Integer, Field> entry : afmResult._effectiveMap1.entrySet()) {
//                int startingBit = entry.getKey();
//                Field f1 = entry.getValue();
//                Field f2 = afmResult._effectiveMap2.get(startingBit);
//                FieldDescriptor fd = f1._fieldDescriptor;
//
//                long intrinstic1 = extendSign(f1._intrinsicValue, fd._fieldSize);
//                long intrinstic2 = extendSign(f2._intrinsicValue, fd._fieldSize);
//                OnesComplement.add36(intrinstic1, intrinstic2, ar);
//                if (ar._overflow) {
//                    diagnostics.append(new TruncationDiagnostic(locale, String.format("Truncation in field %s", fd)));
//                }
//
//                //  normalize the UR's - let any inverses drop out
//                List<UndefinedReference> allRefs = Arrays.asList(f1._undefinedReferences);
//                allRefs.addAll(Arrays.asList(f2._undefinedReferences));
//                List<UndefinedReference> normRefs = normalizeUndefinedReferences(allRefs);
//
//                resultFields[rfx++] = new Field(fd, ar._sum, normRefs.toArray(new UndefinedReference[0]));
//            }
//
//            return new IntegerValue(false, resultFields);
//        } catch (InvalidParameterException ex) {
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    /**
//     * Perform a logical AND of two IntegerValue objects, observing attached forms and relocation information (if any)
//     * @param operand1 left-hand operand
//     * @param operand2 right-hand operand
//     * @param locale location of source code in case we need to raise a diagnostic
//     * @param diagnostics container of diagnostics in case we need to raise on
//     * @return new IntegerValue object representing the logical AND of the two operands
//     */
//    public static IntegerValue and(
//        final IntegerValue operand1,
//        final IntegerValue operand2,
//        final Locale locale,
//        final Diagnostics diagnostics
//    ) {
//        AdjustedFieldMapsResult afmResult = adjustFieldMaps(operand1._fields, operand2._fields);
//        if (afmResult._hasUnequalForms) {
//            diagnostics.append(new FormDiagnostic(locale));
//        }
//
//        if (hasUndefinedReferences(afmResult._effectiveMap1) || hasUndefinedReferences(afmResult._effectiveMap2)) {
//            diagnostics.append(new RelocationDiagnostic(locale));
//        }
//
//        Field[] resultFields = new Field[operand1._fields.size()];
//        int rfx = 0;
//        try {
//            for (Map.Entry<Integer, Field> entry : afmResult._effectiveMap1.entrySet()) {
//                int startingBit = entry.getKey();
//                Field f1 = entry.getValue();
//                Field f2 = afmResult._effectiveMap2.get(startingBit);
//                FieldDescriptor fd = f1._fieldDescriptor;
//
//                long resultValue = f1._intrinsicValue & f2._intrinsicValue;
//                resultFields[rfx++] = new Field(fd, resultValue, new UndefinedReference[0]);
//            }
//
//            return new IntegerValue(false, resultFields);
//        } catch (InvalidParameterException ex) {
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    /**
//     * Perform a division of two IntegerValue objects, observing attached forms and relocation information (if any)
//     * @param operand1 left-hand operand
//     * @param operand2 right-hand operand
//     * @param locale location of source code in case we need to raise a diagnostic
//     * @param diagnostics container of diagnostics in case we need to raise on
//     * @return new IntegerValue object representing the logical OR of the two operands
//     * @throws ExpressionException on division by zero
//     */
//    public static DivisionResult divide(
//        final IntegerValue operand1,
//        final IntegerValue operand2,
//        final Locale locale,
//        final Diagnostics diagnostics
//    ) throws ExpressionException {
//        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
//            diagnostics.append(new RelocationDiagnostic(locale));
//        }
//
//        if ((operand1._fields.size() > 1) || (operand2._fields.size() > 1)) {
//            diagnostics.append(new FormDiagnostic(locale));
//        }
//
//        long intValue1 = operand1.getIntrinsicValue();
//        long intValue2 = operand2.getIntrinsicValue();
//        if (intValue2 == 0) {
//            diagnostics.append(new TruncationDiagnostic(locale, "Division by zero"));
//            throw new ExpressionException();
//        }
//
//        DivisionResult dres = new DivisionResult();
//        long intQuotient = intValue1 / intValue2;
//        long intRemainder = intValue1 % intValue2;
//        long intCovered = intRemainder == 0 ? intQuotient : intQuotient + 1;
//        dres._quotient = new IntegerValue(intQuotient);
//        dres._remainder = new IntegerValue(intRemainder);
//        dres._coveredQuotient = new IntegerValue(intCovered);
//
//        return dres;
//    }
//
//    /**
//     * Indicates number of fields attached to this value
//     */
//    public int getFieldCount() {
//        return _fields.size();
//    }
//
//    /**
//     * Composes the overall intrinsic value of this IV by masking together the intrinsic values
//     * of all the component fields.
//     */
//    public long getIntrinsicValue() {
//        return getIntrinsicValueSum(_fields);
//    }
//
//    /**
//     * Quick indicator of whether this IV has any attached undefined references
//     */
//    public boolean hasUndefinedReferences() {
//        return hasUndefinedReferences(_fields);
//    }
//
//    /**
//     * Multiply two IntegerValue objects, observing attached forms and relocation information (if any)
//     * @param operand1 left-hand operand
//     * @param operand2 right-hand operand
//     * @param locale location of source code in case we need to raise a diagnostic
//     * @param diagnostics container of diagnostics in case we need to raise on
//     * @return new IntegerValue object representing the logical OR of the two operands
//     */
//    public static IntegerValue multiply(
//        final IntegerValue operand1,
//        final IntegerValue operand2,
//        final Locale locale,
//        final Diagnostics diagnostics
//    ) {
//        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
//            diagnostics.append(new RelocationDiagnostic(locale));
//        }
//
//        if ((operand1._fields.size() > 1) || (operand2._fields.size() > 1)) {
//            diagnostics.append(new FormDiagnostic(locale));
//        }
//
//        long intValue1 = operand1.getIntrinsicValue();
//        long intValue2 = operand2.getIntrinsicValue();
//        return new IntegerValue(intValue1 * intValue2);
//    }
//
//    /**
//     * Produces an IV which is the additive inverse of this one
//     */
//    public IntegerValue negate() {
//        Field[] negFields = new Field[_fields.values().size()];
//        int fx = 0;
//        for (Field f : _fields.values()) {
//            negFields[fx++] = f.negate();
//        }
//
//        try {
//            return new IntegerValue(false, negFields);
//        } catch (InvalidParameterException ex) {
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    /**
//     * Perform a logical OR of two IntegerValue objects, observing attached forms and relocation information (if any)
//     * @param operand1 left-hand operand
//     * @param operand2 right-hand operand
//     * @param locale location of source code in case we need to raise a diagnostic
//     * @param diagnostics container of diagnostics in case we need to raise on
//     * @return new IntegerValue object representing the logical OR of the two operands
//     */
//    public static IntegerValue or(
//        final IntegerValue operand1,
//        final IntegerValue operand2,
//        final Locale locale,
//        final Diagnostics diagnostics
//    ) {
//        AdjustedFieldMapsResult afmResult = adjustFieldMaps(operand1._fields, operand2._fields);
//        if (afmResult._hasUnequalForms) {
//            diagnostics.append(new FormDiagnostic(locale));
//        }
//
//        if (hasUndefinedReferences(afmResult._effectiveMap1) || hasUndefinedReferences(afmResult._effectiveMap2)) {
//            diagnostics.append(new RelocationDiagnostic(locale));
//        }
//
//        Field[] resultFields = new Field[operand1._fields.size()];
//        int rfx = 0;
//        try {
//            for (Map.Entry<Integer, Field> entry : afmResult._effectiveMap1.entrySet()) {
//                int startingBit = entry.getKey();
//                Field f1 = entry.getValue();
//                Field f2 = afmResult._effectiveMap2.get(startingBit);
//                FieldDescriptor fd = f1._fieldDescriptor;
//
//                long resultValue = f1._intrinsicValue | f2._intrinsicValue;
//                resultFields[rfx++] = new Field(fd, resultValue, new UndefinedReference[0]);
//            }
//
//            return new IntegerValue(false, resultFields);
//        } catch (InvalidParameterException ex) {
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    /**
//     * Perform a logical XOR of two IntegerValue objects, observing attached forms and relocation information (if any)
//     * @param operand1 left-hand operand
//     * @param operand2 right-hand operand
//     * @param locale location of source code in case we need to raise a diagnostic
//     * @param diagnostics container of diagnostics in case we need to raise on
//     * @return new IntegerValue object representing the logical XOR of the two operands
//     */
//    public static IntegerValue xor(
//        final IntegerValue operand1,
//        final IntegerValue operand2,
//        final Locale locale,
//        final Diagnostics diagnostics
//    ) {
//        AdjustedFieldMapsResult afmResult = adjustFieldMaps(operand1._fields, operand2._fields);
//        if (afmResult._hasUnequalForms) {
//            diagnostics.append(new FormDiagnostic(locale));
//        }
//
//        if (hasUndefinedReferences(afmResult._effectiveMap1) || hasUndefinedReferences(afmResult._effectiveMap2)) {
//            diagnostics.append(new RelocationDiagnostic(locale));
//        }
//
//        Field[] resultFields = new Field[operand1._fields.size()];
//        int rfx = 0;
//        try {
//            for (Map.Entry<Integer, Field> entry : afmResult._effectiveMap1.entrySet()) {
//                int startingBit = entry.getKey();
//                Field f1 = entry.getValue();
//                Field f2 = afmResult._effectiveMap2.get(startingBit);
//                FieldDescriptor fd = f1._fieldDescriptor;
//
//                long resultValue = f1._intrinsicValue ^ f2._intrinsicValue;
//                resultFields[rfx++] = new Field(fd, resultValue, new UndefinedReference[0]);
//            }
//
//            return new IntegerValue(false, resultFields);
//        } catch (InvalidParameterException ex) {
//            throw new RuntimeException("Caught " + ex.getMessage());
//        }
//    }
//
//    //  ----------------------------------------------------------------------------------------------------------------------------
//    //  helpful private things
//    //  ----------------------------------------------------------------------------------------------------------------------------
//
//    /**
//     * Checks whether two field maps are compatible in terms of field sizes.
//     * If so, we merely return the maps.
//     * Otherwise, we create two new maps with a single field of 36 bits each,
//     * along with a suitably determined intrinsic value for each.
//     */
//    private static AdjustedFieldMapsResult adjustFieldMaps(
//        final Map<Integer, Field> fieldMap1,
//        final Map<Integer, Field> fieldMap2
//    ) {
//        AdjustedFieldMapsResult result = new AdjustedFieldMapsResult();
//        if (fieldsAreEqual(fieldMap1, fieldMap2)) {
//            result._effectiveMap1.putAll(fieldMap1);
//            result._effectiveMap2.putAll(fieldMap2);
//            result._hasUnequalForms = false;
//            result._lostUndefinedReferences = false;
//        } else {
//            result._hasUnequalForms = true;
//            result._lostUndefinedReferences = hasUndefinedReferences(fieldMap1) || hasUndefinedReferences(fieldMap2);
//            try {
//                Field f1 = new Field(FieldDescriptor.W,
//                                     getIntrinsicValueSum(fieldMap1),
//                                     new UndefinedReference[0]);
//                result._effectiveMap1.put(f1._fieldDescriptor._startingBit, f1);
//
//                Field f2 = new Field(FieldDescriptor.W,
//                                     getIntrinsicValueSum(fieldMap2),
//                                     new UndefinedReference[0]);
//                result._effectiveMap2.put(f2._fieldDescriptor._startingBit, f2);
//            } catch (InvalidParameterException ex) {
//                throw new RuntimeException("Caught " + ex.getMessage());
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * Performs a sign-extension on an integer of any field size from 1 to 36 bits
//     * @param operand original value
//     * @param fieldSize size of the field in bits
//     * @return sign-extended value
//     */
//    private static long extendSign(
//        final long operand,
//        final int fieldSize
//    ) {
//        if (fieldSize < 36) {
//            long checkMask = 01 << (fieldSize - 1);
//            if ((operand & checkMask) != 0) {
//                long extension = 0_777777_777777L << (fieldSize - 1);
//                return (extension | operand) & 0_777777_777777L;
//            }
//        }
//
//        return operand;
//    }
//
//    /**
//     * Checks the fields described in two field maps to see if they are equivalent
//     * @return true if so, false if not
//     */
//    private static boolean fieldsAreEqual(
//        final Map<Integer, Field> fieldMap1,
//        final Map<Integer, Field> fieldMap2
//    ) {
//        Iterator<Map.Entry<Integer, Field>> iter1 = fieldMap1.entrySet().iterator();
//        Iterator<Map.Entry<Integer, Field>> iter2 = fieldMap2.entrySet().iterator();
//        while (iter1.hasNext() && iter2.hasNext()) {
//            Map.Entry<Integer, Field> entry1 = iter1.next();
//            Map.Entry<Integer, Field> entry2 = iter2.next();
//            if (!entry1.getValue().equals(entry2.getValue())) {
//                return false;
//            }
//        }
//
//        return !iter1.hasNext() && !iter2.hasNext();
//    }
//
//    /**
//     * Calculates the intrinsic value of the various fields in a field map,
//     * observing the various field positions.
//     */
//    private static long getIntrinsicValueSum(
//        final Map<Integer, Field> fieldMap
//    ) {
//        long result = 0;
//        for (Field f : fieldMap.values()) {
//            int shift = 36 - (f._fieldDescriptor._startingBit + f._fieldDescriptor._fieldSize);
//            result |= f._intrinsicValue << shift;
//        }
//        return result;
//    }
//
//    /**
//     * Indicates whether any of the fields in the given field map have undefined reference entries attached
//     */
//    private static boolean hasUndefinedReferences(
//        final Map<Integer, Field> fieldMap
//    ) {
//        for (Field f : fieldMap.values()) {
//            if (f._undefinedReferences.length > 0) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    /**
//     * Normalizes a list of undefined references to drop out inverses
//     * i.e., we would lose a positive and negative pair of the otherwise-same undefined labels.
//     * @param source source list
//     * @return normalized list
//     */
//    private static List<UndefinedReference> normalizeUndefinedReferences(
//        final List<UndefinedReference> source
//    ) {
//        Map<UndefinedReference, Integer> temp = new HashMap<>();
//        for (UndefinedReference ur : source) {
//            Integer count = temp.get(ur);
//            if (count == null) {
//                count = 0;
//            }
//
//            count = (ur._isNegative ? count - 1 : count + 1);
//            temp.put(ur, count);
//        }
//
//        List<UndefinedReference> result = new LinkedList<>();
//        for (Map.Entry<UndefinedReference, Integer> entry : temp.entrySet()) {
//            int count = Math.abs(entry.getValue());
//            boolean isNegative = entry.getValue() < 0;
//            for (int c = 0; c < count; ++c) {
//                result.add(entry.getKey().copy(isNegative));
//            }
//        }
//
//        return result;
//    }

    public final Form _form;
    public final UndefinedReference[] _references;
    public final long _value;

    /**
     * general constructor
     * @param flagged - leading asterisk
     * @param value - integer value
     * @param form - an attached form (null if none)
     * @param references - zero or more undefined references (null not allowed)
     */
    public IntegerValue(
        final boolean flagged,
        final long value,
        final Form form,
        final UndefinedReference[] references
    ) {
        super(flagged);
        _form = form;
        _references = Arrays.copyOf(references, references.length);
        _value = value;
    }

    /**
     * constructor for the simplest case
     */
    public IntegerValue(
        final long value
    ) {
        super(false);
        _form = null;
        _references = new UndefinedReference[0];
        _value = value;
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

            if ((_form == null) && (iobj._form != null) && !_form.equals(iobj._form)) {
                throw new FormException();
            } else if (_form != iobj._form) {
                throw new FormException();
            }

            if (!UndefinedReference.equals(_references, iobj._references)) {
                throw new RelocationException();
            }

            return Long.compare(_value, iobj._value);
        } else {
            throw new TypeException();
        }
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
        return new IntegerValue(newFlagged, _value, _form, _references);
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
        if (obj instanceof IntegerValue) {
            IntegerValue iv = (IntegerValue) obj;
            return (_value == iv._value)
                   && (_form.equals(iv._form)
                   && UndefinedReference.equals(_references, iv._references));
        }

        return false;
    }

    @Override
    public ValueType getType() { return ValueType.Integer; }

    @Override
    public int hashCode() { return (int) _value; }

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
        if (_form != null) {
            diagnostics.append(new ErrorDiagnostic(locale, "Integer value has a form attached"));
        }

        return new FloatingPointValue(false, _value);
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
        return new IntegerValue(false, _value, _form, _references);
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
        if (_form != null) {
            diagnostics.append(new ErrorDiagnostic(locale, "Integer value has a form attached"));
        }

        if (characterMode == CharacterMode.ASCII) {
            return new StringValue(false, Word36.toASCII(_value), characterMode);
        } else {
            return new StringValue(false, Word36.toFieldata(_value), characterMode);
        }
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%s%d",
                                _flagged ? "*" : "",
                                _form == null ? "" : _form.toString(),
                                _value));
        for (UndefinedReference ur : _references) {
            sb.append(ur.toString());
        }

        return sb.toString();
    }
}
