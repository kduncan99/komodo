/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.oldbaselib.FieldDescriptor;
import com.kadware.komodo.kex.kasm.*;
import com.kadware.komodo.kex.kasm.diagnostics.*;
import com.kadware.komodo.kex.kasm.exceptions.*;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * A Value which represents a 72-bit ones-complement signed integer expressed as a DoubleWord36 object.
 * Unlike other *Value objects, this one can have a form and multiple undefined reference objects attached.
 */
@SuppressWarnings("Duplicates")
public class IntegerValue extends Value {

    public static class DivisionResult {
        public final IntegerValue _quotient;
        public final IntegerValue _remainder;
        public final IntegerValue _coveredQuotient;

        DivisionResult(
            final IntegerValue quotient,
            final IntegerValue remainder,
            final IntegerValue coveredQuotient
        ) {
            _quotient = quotient;
            _remainder = remainder;
            _coveredQuotient = coveredQuotient;
        }
    }

    public static class IntegrateResult {
        public final IntegerValue _value;
        public final Diagnostics _diagnostics;

        public IntegrateResult(
            final IntegerValue value,
            final Diagnostics diagnostics
        ) {
            _value = value;
            _diagnostics = diagnostics;
        }
    }

    //  A useful IntegerValue containing zero, no flags, and no unidentified references.
    public static final IntegerValue POSITIVE_ZERO = new Builder().setValue(new DoubleWord36(DoubleWord36.POSITIVE_ZERO)).build();

    public final Form _form;
    public final UnresolvedReference[] _references;
    public final DoubleWord36 _value;

    /**
     * general constructor
     * @param locale - where this was defined
     * @param flagged - leading asterisk
     * @param value - ones-complement integer value
     * @param precision - single, double, or default (used for generating code)
     * @param form - an attached form (null if none)
     * @param references - zero or more undefined references (null not allowed)
     */
    private IntegerValue(
        final Locale locale,
        final boolean flagged,
        final DoubleWord36 value,
        final ValuePrecision precision,
        final Form form,
        final UnresolvedReference[] references
    ) {
        super(locale, flagged, precision);
        _value = value;
        _form = form;
        _references = Arrays.copyOf(references, references.length);
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

            if (!UnresolvedReference.equals(_references, iobj._references)) {
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
     * @param locale new value for Locale
     * @param newFlagged new attribute value
     * @return new value
     */
    @Override
    public Value copy(
        final Locale locale,
        final boolean newFlagged
    ) {
        return new IntegerValue(locale, newFlagged, _value, _precision, _form, _references);
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param locale new value for Locale
     * @param newPrecision new value for precision attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final Locale locale,
        final ValuePrecision newPrecision
    ) {
        return new IntegerValue(locale, _flagged, _value, newPrecision, _form, _references);
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

            return UnresolvedReference.equals(_references, iv._references);
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
        for (UnresolvedReference ur : _references) {
            sb.append(ur.toString());
            sb.append(ur._fieldDescriptor.toString());
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
     * @param assembler in case we need to raise a diagnostic
     * @return new IntegerValue object representing the sum of the two addends
     */
    public static IntegerValue add(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Assembler assembler
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        boolean ignoreReferenceFields = false;
        if (!checkValueForms(operand1, operand2)) {
            assembler.appendDiagnostic(new FormDiagnostic(locale));
            if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
                assembler.appendDiagnostic(new RelocationDiagnostic(locale));
                ignoreReferenceFields = true;
            }
            resultForm = null;
        }

        //  If there is no form for the result, the process is fairly straight-forward
        BigInteger biResult;
        if (resultForm == null) {
            DoubleWord36.StaticAdditionResult sar = DoubleWord36.add(operand1._value.get(), operand2._value.get());
            if (sar._overflow) {
                assembler.appendDiagnostic(new TruncationDiagnostic(locale, "LinkResult of addition is truncated"));
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
                    String msg = String.format("LinkResult of addition is truncated in field %s", fd.toString());
                    assembler.appendDiagnostic(new TruncationDiagnostic(locale, msg));
                }

                biResult = biResult.or(sar._value);
            }
        }

        //  Now deal with the undefined references (if any).
        UnresolvedReference[] resultRefs = new UnresolvedReference[operand1._references.length + operand2._references.length];
        int tx = 0;
        for (UnresolvedReference ur : operand1._references) {
            resultRefs[tx++] = ignoreReferenceFields ? ur.copy(FieldDescriptor.W) : ur;
        }
        for (UnresolvedReference ur : operand2._references) {
            resultRefs[tx++] = ignoreReferenceFields ? ur.copy(FieldDescriptor.W) : ur;
        }
        resultRefs = UnresolvedReference.coalesce(resultRefs);

        return new IntegerValue.Builder().setLocale(locale)
                                         .setValue(new DoubleWord36(biResult))
                                         .setForm(resultForm)
                                         .setReferences(resultRefs)
                                         .build();
    }

    /**
     * Perform a logical AND of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param assembler in case we need to raise a diagnostic
     * @return new IntegerValue object representing the logical AND of the two operands
     */
    public static IntegerValue and(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Assembler assembler
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        if (!checkValueForms(operand1, operand2)) {
            assembler.appendDiagnostic(new FormDiagnostic(locale));
            resultForm = null;
        }

        ValuePrecision resultPrecision = ValuePrecision.Default;
        if (operand1._precision == ValuePrecision.Double || operand2._precision == ValuePrecision.Double) {
            resultPrecision = ValuePrecision.Double;
        }

        List<UnresolvedReference> refList = new LinkedList<>();
        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            assembler.appendDiagnostic(new RelocationDiagnostic(locale));
            refList.addAll(Arrays.asList(operand1._references));
            refList.addAll(Arrays.asList(operand2._references));
        }

        DoubleWord36 newValue = operand1._value.logicalAnd(operand2._value);
        return new IntegerValue.Builder().setLocale(locale)
                                         .setValue(newValue)
                                         .setPrecision(resultPrecision)
                                         .setForm(resultForm)
                                         .setReferences(refList)
                                         .build();
    }

    /**
     * Create a new copy of this object, with the given form
     * @param newForm new form value
     * @return new value
     */
    public Value copy(
        final Form newForm
    ) {
        return new IntegerValue(_locale, _flagged, _value, _precision, newForm, _references);
    }

    /**
     * Create a new copy of this object, with the given form and locale
     * @param locale new locale value
     * @param newForm new form value
     * @return new value
     */
    public Value copy(
        final Locale locale,
        final Form newForm
    ) {
        return new IntegerValue(locale, _flagged, _value, _precision, newForm, _references);
    }

    /**
     * Perform a division of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param assembler in case we need to raise a diagnostic
     * @return new IntegerValue object representing the logical OR of the two operands
     * @throws ExpressionException on division by zero
     */
    public static DivisionResult divide(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Assembler assembler
    ) throws ExpressionException {
        if ((operand1._form != null) || (operand2._form != null)) {
            assembler.appendDiagnostic(new FormDiagnostic(locale));
        }

        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            assembler.appendDiagnostic(new RelocationDiagnostic(locale));
        }

        if (operand2._value.isZero()) {
            assembler.appendDiagnostic(new TruncationDiagnostic(locale, "Division by zero"));
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

        return new DivisionResult(new IntegerValue.Builder().setLocale(locale).setValue(dwQuotient).build(),
                                  new IntegerValue.Builder().setLocale(locale).setValue(dwRemainder).build(),
                                  new IntegerValue.Builder().setLocale(locale).setValue(dwCovered).build());
    }

    /**
     * Quick indicator of whether this IntegerValue has any attached undefined references
     */
    public boolean hasUndefinedReferences() {
        return _references.length > 0;
    }

    /**
     * Integrates a set of component values, governed by corresponding field descriptors, into an initial value.
     * The resulting value is not flagged.
     * The reference set for the resulting value is the union of the initial value's reference set with the
     *      component values' reference sets, suitably shifted to represent the component value field descriptor.
     * The resulting value's form will be the initial value's form, if any.
     * The integration is performed by adding the component value to a particular field of the initial value.
     * This process is designed only for 36-bit integer values. If larger values are given, we might throw a diagnostic,
     *      or we might just work through it - results are largely undefined.
     * Resulting precision is always taken from the initial value (should be default or single, but whatever).
     *
     * So, if we have a base value of
     *      0_111222_333444
     * and two component values of
     *      0_111111 (field corresponding to H1)
     *  and 0_222222 (field corresponding to H2)
     * The resulting value would be
     *      0_222333_555666.
     *
     * @param initialValue the value into which all component values are integrated
     * @param fieldDescriptors one per component value, each describes the field (the subset) of the initial value into which
     *                         the corresponding component value is integrated
     * @param componentValues the component values to be integrated
     * @param locale location of the textual entity which is causing this integration to occur
     * @return resulting value
     */
    public static IntegrateResult integrate(
        final IntegerValue initialValue,
        final FieldDescriptor[] fieldDescriptors,
        final IntegerValue[] componentValues,
        final Locale locale
    ) {
        Diagnostics diags = new Diagnostics();

        if (fieldDescriptors.length != componentValues.length) {
            diags.append(new FatalDiagnostic(locale, "Internal error:Arrays of unequal length in integrateValues()"));
            return new IntegrateResult(initialValue, diags);
        }

        //  All BigInteger values are ones-complement, so we cannot use the standard BigInteger arithmetic
        //  (except in basic mask manipulation).  We use DoubleWord36 arithmetic instead.
        BigInteger workingValue = initialValue._value.get();
        List<UnresolvedReference> references = new LinkedList<>(Arrays.asList(initialValue._references));
        for (int fx = 0; fx < fieldDescriptors.length; ++fx) {
            //  Find the field descriptor info - FDs only apply to 36-bit values.
            //  Develop a shift count for creating various masks.
            FieldDescriptor fd = fieldDescriptors[fx];
            int shiftCount = 36 - (fd._startingBit + fd._fieldSize);

            BigInteger rightShiftedMask = BigInteger.ONE.shiftLeft(fd._fieldSize).subtract(BigInteger.ONE);
            BigInteger rightShiftedMSBit= BigInteger.ONE.shiftLeft(fd._fieldSize - 1);
            BigInteger rightShiftedNotMask = rightShiftedMask.xor(DoubleWord36.BIT_MASK);
            BigInteger positionMask = rightShiftedMask.shiftLeft(shiftCount);
            BigInteger positionNotMask = positionMask.xor(DoubleWord36.BIT_MASK);

            //  Get a subset of the working value corresponding to the field descriptor, shifted right as necessary.
            //  Add the component value, and check whether we've overflowed the allowed field space.
            BigInteger tempValue = workingValue.shiftRight(shiftCount).and(rightShiftedMask);
            if (!(tempValue.and(rightShiftedMSBit)).equals(BigInteger.ZERO)) {
                //  original field value is negative...  sign-extend it.
                tempValue = tempValue.or(rightShiftedNotMask);
            }

            //  A special note - we recognize that the source word is in ones-complement.
            //  The reference value *might* be negative - if that is the case, we have a bit of a dilemma,
            //  as we don't know whether the field we slice out is signed or unsigned.
            //  As it turns out, it doesn't matter.  We treat it as signed, sign-extend it if it is
            //  negative, convert to twos-complement, add or subtract the reference, then convert it
            //  back to ones-complement.  This works regardless, via magic.

            tempValue = DoubleWord36.addSimple(tempValue, componentValues[fx]._value.get());

            //  Check for field overflow...
            boolean trunc;
            if (DoubleWord36.isPositive(tempValue)) {
                trunc = !tempValue.and(rightShiftedNotMask).equals(BigInteger.ZERO);
            } else {
                trunc = !tempValue.or(rightShiftedMask).equals(DoubleWord36.BIT_MASK);
            }

            if (trunc) {
                String msg = String.format("Truncating value %01o in subfield %s", tempValue, fd.toString());
                diags.append(new TruncationDiagnostic(locale, msg));
            }

            //  splice the temporary subvalue back into the working value.
            workingValue = workingValue.and(positionNotMask).or(tempValue.and(rightShiftedMask).shiftLeft(shiftCount));

            //  Any references to look at?
            for (UnresolvedReference ur : componentValues[fx]._references) {
                references.add(ur.copy(fd));
            }
        }

        IntegerValue resultValue = new IntegerValue.Builder().setLocale(locale)
                                                             .setValue(workingValue)
                                                             .setForm(initialValue._form)
                                                             .setPrecision(initialValue._precision)
                                                             .setReferences(references)
                                                             .build();
        return new IntegrateResult(resultValue, diags);
    }

    /**
     * Multiply two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param assembler in case we need to raise a diagnostic
     * @return new IntegerValue object representing the logical OR of the two operands
     */
    public static IntegerValue multiply(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Assembler assembler
    ) {
        if ((operand1._form != null) || (operand2._form != null)) {
            assembler.appendDiagnostic(new FormDiagnostic(locale));
        }

        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            assembler.appendDiagnostic(new RelocationDiagnostic(locale));
        }

        DoubleWord36.MultiplicationResult mr = operand1._value.multiply(operand2._value);
        if (mr._overflow) {
            assembler.appendDiagnostic(new TruncationDiagnostic(locale, "LinkResult of multiplication too large"));
        }

        DoubleWord36 dw = new DoubleWord36(mr._value);
        return new IntegerValue.Builder().setLocale(locale).setValue(dw).build();
    }

    /**
     * Produces an IV which is the additive inverse of this one
     */
    public IntegerValue negate(
        final Locale locale
    ) {
        UnresolvedReference[] negRefs = new UnresolvedReference[_references.length];
        for (int ux = 0; ux < _references.length; ++ux) {
            negRefs[ux] = _references[ux].copy(!_references[ux]._isNegative);
        }

        DoubleWord36 newValue = _value.negate();
        return new IntegerValue(locale, _flagged, newValue, _precision, _form, negRefs);

    }

    /**
     * Produces an IV which is the logical inverse of this one
     * @param locale location of source code in case we need to raise a diagnostic
     * @param assembler in case we need to raise a diagnostic
     */
    public IntegerValue not(
        final Locale locale,
        final Assembler assembler
    ) {
        if (_references.length > 0) {
            assembler.appendDiagnostic(new RelocationDiagnostic(locale));
        }

        DoubleWord36 newValue = _value.negate();
        return new IntegerValue(locale, _flagged, newValue, _precision, _form, _references);
    }

    /**
     * Perform a logical OR of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param assembler in case we need to raise a diagnostic
     * @return new IntegerValue object representing the logical OR of the two operands
     */
    public static IntegerValue or(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Assembler assembler
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        if (!checkValueForms(operand1, operand2)) {
            assembler.appendDiagnostic(new FormDiagnostic(locale));
            resultForm = null;
        }

        ValuePrecision resultPrecision = ValuePrecision.Default;
        if (operand1._precision == ValuePrecision.Double || operand2._precision == ValuePrecision.Double) {
            resultPrecision = ValuePrecision.Double;
        }

        List<UnresolvedReference> refList = new LinkedList<>();
        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            assembler.appendDiagnostic(new RelocationDiagnostic(locale));
            refList.addAll(Arrays.asList(operand1._references));
            refList.addAll(Arrays.asList(operand2._references));
        }

        DoubleWord36 newValue = operand1._value.logicalOr(operand2._value);
        return new IntegerValue(locale,
                                false,
                                newValue,
                                resultPrecision,
                                resultForm,
                                refList.toArray(new UnresolvedReference[0]));
    }

    /**
     * Perform a logical XOR of two IntegerValue objects, observing attached forms and relocation information (if any)
     * @param operand1 left-hand operand
     * @param operand2 right-hand operand
     * @param locale location of source code in case we need to raise a diagnostic
     * @param assembler in case we need to raise a diagnostic
     * @return new IntegerValue object representing the logical XOR of the two operands
     */
    public static IntegerValue xor(
        final IntegerValue operand1,
        final IntegerValue operand2,
        final Locale locale,
        final Assembler assembler
    ) {
        //  If forms match, use the left-hand form in the result - otherwise, the result has no attached form
        Form resultForm = operand1._form;
        if (!checkValueForms(operand1, operand2)) {
            assembler.appendDiagnostic(new FormDiagnostic(locale));
            resultForm = null;
        }

        ValuePrecision resultPrecision = ValuePrecision.Default;
        if (operand1._precision == ValuePrecision.Double || operand2._precision == ValuePrecision.Double) {
            resultPrecision = ValuePrecision.Double;
        }

        List<UnresolvedReference> refList = new LinkedList<>();
        if (operand1.hasUndefinedReferences() || operand2.hasUndefinedReferences()) {
            assembler.appendDiagnostic(new RelocationDiagnostic(locale));
            refList.addAll(Arrays.asList(operand1._references));
            refList.addAll(Arrays.asList(operand2._references));
        }

        DoubleWord36 newValue = operand1._value.logicalXor(operand2._value);
        return new IntegerValue(locale,
                                false,
                                newValue,
                                resultPrecision,
                                resultForm,
                                refList.toArray(new UnresolvedReference[0]));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  helpful private things
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Compares forms attached to the given values (if existing) for equality/compatibility.
     * If neither value has an attached form, the test passes.
     * @return true if the forms are equivalent, else false
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
        Locale _locale = null;
        ValuePrecision _precision = ValuePrecision.Default;
        UnresolvedReference[] _references = new UnresolvedReference[0];
        DoubleWord36 _value = null;

        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }
        public Builder setForm(Form value)                          { _form = value; return this; }
        public Builder setLocale(Locale value)                      { _locale = value; return this; }
        public Builder setPrecision(ValuePrecision value)           { _precision = value; return this; }
        public Builder setReferences(UnresolvedReference[] values)   { _references = Arrays.copyOf(values, values.length); return this; }

        public Builder setReferences(
            Collection<UnresolvedReference> values
        ) {
            _references = values.toArray(new UnresolvedReference[0]);
            return this;
        }

        public Builder setValue(BigInteger value)                   { _value = new DoubleWord36(value); return this; }
        public Builder setValue(DoubleWord36 value)                 { _value = value; return this; }
        public Builder setValue(Word36 value)                       { _value = new DoubleWord36(0, value.getW()); return this; }
        public Builder setValue(long value)                         { _value = new DoubleWord36(0, value); return this; }

        public IntegerValue build(
        ) {
            if (_value == null) {
                throw new RuntimeException("Value not specified for IntegerValue builder");
            }

            return new IntegerValue(_locale, _flagged, _value, _precision, _form, _references);
        }
    }
}
