/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.baselib.DoubleWord36;
import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * A Value which represents a 36-bit signed integer. Note that this differs from the way MASM works.
 * Unlike other *Value objects, this one can have a form and multiple undefined reference objects attached.
 */
@SuppressWarnings("Duplicates")
public class IntegerValue extends Value {

    public static class DivisionResult {
        public final IntegerValue _quotient;
        public final IntegerValue _remainder;
        public final IntegerValue _coveredQuotient;

        public DivisionResult(
            final IntegerValue quotient,
            final IntegerValue remainder,
            final IntegerValue coveredQuotient
        ) {
            _quotient = quotient;
            _remainder = remainder;
            _coveredQuotient = coveredQuotient;
        }
    }

    //  A useful IntegerValue containing zero, no flags, and no unidentified references.
    //  Also, a negative version thereof.
    public static final IntegerValue NEGATIVE_ZERO = new Builder().setValue(new DoubleWord36(DoubleWord36.NEGATIVE_ZERO)).build();
    public static final IntegerValue POSITIVE_ZERO = new Builder().setValue(new DoubleWord36(DoubleWord36.POSITIVE_ZERO)).build();

    public final Form _form;
    public final UndefinedReference[] _references;
    public final DoubleWord36 _value;

    /**
     * general constructor
     * @param flagged - leading asterisk
     * @param value - ones-complement integer value
     * @param precision - single, double, or default (used for generating code)
     * @param form - an attached form (null if none)
     * @param references - zero or more undefined references (null not allowed)
     */
    protected IntegerValue(
        final boolean flagged,
        final DoubleWord36 value,
        final ValuePrecision precision,
        final Form form,
        final UndefinedReference[] references
    ) {
        super(flagged, precision);
        _form = form;
        _references = Arrays.copyOf(references, references.length);
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

            if ((_form != null) && (iobj._form != null) && !_form.equals(iobj._form)) {
                throw new FormException();
            } else if (_form != iobj._form) {
                throw new FormException();
            }

            if (!UndefinedReference.equals(_references, iobj._references)) {
                throw new RelocationException();
            }

            //  Convert to twos-complement and compare
            BigInteger thisTwos = _value.getTwosComplement();
            BigInteger thatTwos = iobj._value.getTwosComplement();
            return thisTwos.compareTo(thatTwos);
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
        return new IntegerValue(newFlagged, _value, _precision, _form, _references);
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
            if (!_value.equals(iv._value)) { return false; }
            if (_form != null) {
                if (iv._form == null) { return false; }
                if (!_form.equals(iv._form)) { return false; }
            } else if (iv._form != null) { return false; }

            return UndefinedReference.equals(_references, iv._references);
        }

        return false;
    }

    @Override public ValueType getType() { return ValueType.Integer; }
    @Override public int hashCode() { return _value.hashCode(); }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%s%s",
                                _flagged ? "*" : "",
                                _form == null ? "" : _form.toString(),
                                _value.toString()));
        for (UndefinedReference ur : _references) {
            sb.append(ur.toString());
        }

        return sb.toString();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Stuff specific to IntegerValue
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Add two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param diagnostics container of diagnostics in case we need to raise on
     * @return new IntegerValue object representing the sum of the two addends
     */
    public static IntegerValue add(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Diagnostics diagnostics
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        boolean ignoreReferenceFields = false;
        if (!checkValueForms(operand1, operand2)) {
            diagnostics.append(new FormDiagnostic(locale));
            if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
                diagnostics.append(new RelocationDiagnostic(locale));
                ignoreReferenceFields = true;
            }
            resultForm = null;
        }

        //  If there is no form for the result, the process is fairly straight-forward
        BigInteger biResult;
        if (resultForm == null) {
            DoubleWord36.StaticAdditionResult sar = DoubleWord36.add(operand1._value.get(), operand2._value.get());
            if (sar._overflow) {
                diagnostics.append(new TruncationDiagnostic(locale, "Result of addition is truncated"));
            }
            biResult = sar._value;
        } else {
            //  We have to do the additions per-field, and they have to be ones-complement signed regardless of the field size.
            BigInteger bi1 = operand1._value.get();
            BigInteger bi2 = operand2._value.get();
            biResult = BigInteger.ZERO;
            FieldDescriptor[] fds = resultForm.getFieldDescriptors();
            for (FieldDescriptor fd : fds) {
                int shift = 36 - (fd._startingBit + fd._fieldSize);
                BigInteger mask = BigInteger.ONE.shiftLeft(fd._fieldSize).subtract(BigInteger.ONE);
                BigInteger notMask = mask.not().and(DoubleWord36.BIT_MASK);

                BigInteger temp1 = DoubleWord36.extendSign(bi1.shiftRight(shift).and(mask), fd._fieldSize);
                BigInteger temp2 = DoubleWord36.extendSign(bi2.shiftRight(shift).and(mask), fd._fieldSize);
                DoubleWord36.StaticAdditionResult sar = DoubleWord36.add(temp1, temp2);
                if (sar._overflow || !((sar._value.and(notMask)).equals(BigInteger.ZERO))) {
                    String msg = String.format("Result of addition is truncated in field %s", fd.toString());
                    diagnostics.append(new TruncationDiagnostic(locale, msg));
                }

                biResult = biResult.or(sar._value);
            }
        }

        //  Now deal with the undefined references (if any).
        UndefinedReference[] resultRefs = new UndefinedReference[operand1._references.length + operand2._references.length];
        int tx = 0;
        for (UndefinedReference ur : operand1._references) {
            resultRefs[tx++] = ignoreReferenceFields ? ur.copy(FieldDescriptor.W) : ur;
        }
        for (UndefinedReference ur : operand2._references) {
            resultRefs[tx++] = ignoreReferenceFields ? ur.copy(FieldDescriptor.W) : ur;
        }
        resultRefs = UndefinedReference.coalesce(resultRefs);

        return new IntegerValue.Builder().setValue(new DoubleWord36(biResult))
                                         .setForm(resultForm)
                                         .setReferences(resultRefs)
                                         .build();
    }

    /**
     * Perform a logical AND of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param diagnostics container of diagnostics in case we need to raise on
     * @return new IntegerValue object representing the logical AND of the two operands
     */
    public static IntegerValue and(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Diagnostics diagnostics
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        if (!checkValueForms(operand1, operand2)) {
            diagnostics.append(new FormDiagnostic(locale));
            resultForm = null;
        }

        List<UndefinedReference> refList = new LinkedList<>();
        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            diagnostics.append(new RelocationDiagnostic(locale));
            refList.addAll(Arrays.asList(operand1._references));
            refList.addAll(Arrays.asList(operand2._references));
        }

        DoubleWord36 newValue = operand1._value.logicalAnd(operand2._value);
        return new IntegerValue.Builder().setForm(resultForm)
                                         .setReferences(refList.toArray(new UndefinedReference[0]))
                                         .setValue(newValue)
                                         .build();
    }

    /**
     * Perform a division of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param diagnostics container of diagnostics in case we need to raise on
     * @return new IntegerValue object representing the logical OR of the two operands
     * @throws ExpressionException on division by zero
     */
    public static DivisionResult divide(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Diagnostics diagnostics
    ) throws ExpressionException {
        if ((operand1._form != null) || (operand2._form != null)) {
            diagnostics.append(new FormDiagnostic(locale));
        }

        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            diagnostics.append(new RelocationDiagnostic(locale));
        }

        if (operand2._value.isZero()) {
            diagnostics.append(new TruncationDiagnostic(locale, "Division by zero"));
            throw new ExpressionException();
        }

        DoubleWord36.DivisionResult dres = operand1._value.divide(operand2._value);
        DoubleWord36 dwQuotient = new DoubleWord36(dres._result);
        DoubleWord36 dwRemainder = new DoubleWord36(dres._remainder);
        DoubleWord36 dwCovered;
        if (dwRemainder.isZero()) {
            dwCovered = dwRemainder;
        } else {
            dwCovered = new DoubleWord36(DoubleWord36.addSimple(dwRemainder.get(), BigInteger.ONE));
        }

        return new DivisionResult(new IntegerValue.Builder().setValue(dwQuotient).build(),
                                  new IntegerValue.Builder().setValue(dwRemainder).build(),
                                  new IntegerValue.Builder().setValue(dwCovered).build());
    }

    /**
     * Quick indicator of whether this IntegerValue has any attached undefined references
     */
    public boolean hasUndefinedReferences() {
        return _references.length > 0;
    }

    /**
     * Multiply two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param diagnostics container of diagnostics in case we need to raise on
     * @return new IntegerValue object representing the logical OR of the two operands
     */
    public static IntegerValue multiply(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Diagnostics diagnostics
    ) {
        if ((operand1._form != null) || (operand2._form != null)) {
            diagnostics.append(new FormDiagnostic(locale));
        }

        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            diagnostics.append(new RelocationDiagnostic(locale));
        }

        DoubleWord36.MultiplicationResult mr = operand1._value.multiply(operand2._value);
        if (mr._overflow) {
            diagnostics.append(new TruncationDiagnostic(locale, "Result of multiplication too large"));
        }

        DoubleWord36 dw = new DoubleWord36(mr._value);
        return new IntegerValue.Builder().setValue(dw).build();
    }

    /**
     * Produces an IV which is the additive inverse of this one
     */
    public IntegerValue negate() {
        UndefinedReference[] negRefs = new UndefinedReference[_references.length];
        for (int ux = 0; ux < _references.length; ++ux) {
            negRefs[ux] = _references[ux].copy(!_references[ux]._isNegative);
        }

        DoubleWord36 newValue = _value.negate();
        return new IntegerValue.Builder().setValue(newValue)
                                         .setForm(_form)
                                         .setReferences(negRefs)
                                         .setPrecision(_precision)
                                         .build();
    }

    /**
     * Produces an IV which is the logical inverse of this one
     */
    public IntegerValue not(
        final Locale locale,
        final Diagnostics diagnostics
    ) {
        if (_references.length > 0) {
            diagnostics.append(new RelocationDiagnostic(locale));
        }

        DoubleWord36 newValue = _value.negate();
        return new IntegerValue.Builder().setValue(newValue)
                                         .setForm(_form)
                                         .setReferences(_references)
                                         .setPrecision(_precision)
                                         .build();
    }

    /**
     * Perform a logical OR of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param diagnostics container of diagnostics in case we need to raise on
     * @return new IntegerValue object representing the logical OR of the two operands
     */
    public static IntegerValue or(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Diagnostics diagnostics
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        if (!checkValueForms(operand1, operand2)) {
            diagnostics.append(new FormDiagnostic(locale));
            resultForm = null;
        }

        List<UndefinedReference> refList = new LinkedList<>();
        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            diagnostics.append(new RelocationDiagnostic(locale));
            refList.addAll(Arrays.asList(operand1._references));
            refList.addAll(Arrays.asList(operand2._references));
        }

        DoubleWord36 newValue = operand1._value.logicalOr(operand2._value);
        return new IntegerValue.Builder().setForm(resultForm)
                                         .setReferences(refList.toArray(new UndefinedReference[0]))
                                         .setValue(newValue)
                                         .build();
    }

    /**
     * Perform a logical XOR of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param diagnostics container of diagnostics in case we need to raise on
     * @return new IntegerValue object representing the logical XOR of the two operands
     */
    public static IntegerValue xor(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Diagnostics diagnostics
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        if (!checkValueForms(operand1, operand2)) {
            diagnostics.append(new FormDiagnostic(locale));
            resultForm = null;
        }

        List<UndefinedReference> refList = new LinkedList<>();
        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            diagnostics.append(new RelocationDiagnostic(locale));
            refList.addAll(Arrays.asList(operand1._references));
            refList.addAll(Arrays.asList(operand2._references));
        }

        DoubleWord36 newValue = operand1._value.logicalXor(operand2._value);
        return new IntegerValue.Builder().setForm(resultForm)
                                         .setReferences(refList.toArray(new UndefinedReference[0]))
                                         .setValue(newValue)
                                         .build();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  helpful private things
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Compares forms attached to the given values (if existing) for equality.
     * If neither value has an attached form, the test passes.
     * @return true if the forms are equivalent, else false
     */
    private static boolean checkValueForms(
        final IntegerValue value1,
        final IntegerValue value2
    ) {
        if (value1._form == null) {
            return value2._form == null;
        } else {
            if (value2._form == null) {
                return false;
            } else {
                return value1._form.equals(value2._form);
            }
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        boolean _flagged = false;
        Form _form = null;
        ValuePrecision _precision = ValuePrecision.Default;
        UndefinedReference[] _references = new UndefinedReference[0];
        DoubleWord36 _value = null;

        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }
        public Builder setForm(Form value)                          {_form = value; return this; }
        public Builder setPrecision(ValuePrecision value)           { _precision = value; return this; }
        public Builder setReferences(UndefinedReference[] values)   { _references = Arrays.copyOf(values, values.length); return this; }
        public Builder setValue(DoubleWord36 value)                 { _value = value; return this; }
        public Builder setValue(Word36 value)                       { _value = new DoubleWord36(0, value.getW()); return this; }
        public Builder setValue(long value)                         { _value = new DoubleWord36(0, value); return this; }

        public IntegerValue build(
        ) {
            if (_value == null) {
                throw new RuntimeException("Value not specified for IntegerValue builder");
            }

            return new IntegerValue(_flagged, _value, _precision, _form, _references);
        }
    }
}
