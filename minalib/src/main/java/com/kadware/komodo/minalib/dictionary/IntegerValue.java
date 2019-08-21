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
import java.util.LinkedList;
import java.util.List;

/**
 * A Value which represents a 36-bit signed integer. Note that this differs from the way MASM works.
 * Unlike other *Value objects, this one can have a form and multiple undefined reference objects attached.
 */
@SuppressWarnings("Duplicates")
public class IntegerValue extends Value {

    public static class DivisionResult {
        public IntegerValue _quotient;
        public IntegerValue _remainder;
        public IntegerValue _coveredQuotient;
    }

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

            if ((_form != null) && (iobj._form != null) && !_form.equals(iobj._form)) {
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
    ) throws TypeException {
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
    ) throws TypeException {
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
    ) throws TypeException {
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
        OnesComplement.Add36Result ar = new OnesComplement.Add36Result();
        long intrinsic = 0;
        if (resultForm == null) {
            OnesComplement.add36(operand1._value, operand2._value, ar);
            if (ar._overflow) {
                diagnostics.append(new TruncationDiagnostic(locale, "Result of addition is truncated"));
            }
            intrinsic = ar._sum;
        } else {
            //  We have to do the additions per-field
            FieldDescriptor[] fds = resultForm.getFieldDescriptors();
            for (FieldDescriptor fd : fds) {
                int shift = 36 - (fd._startingBit + fd._fieldSize);
                long mask = (1 << fd._fieldSize) - 1;
                long notMask = ~mask;

                long temp1 = (operand1._value >> shift) & mask;
                long temp2 = (operand2._value >> shift) & mask;
                OnesComplement.add36(temp1, temp2, ar);
                if (ar._overflow || ((ar._sum & notMask) != 0)) {
                    String msg = String.format("Result of addition is truncated in field %s", fd.toString());
                    diagnostics.append(new TruncationDiagnostic(locale, msg));
                }

                intrinsic |= ar._sum;
            }
        }

        //  Now deal with the undefined references (if any).
        UndefinedReference[] temp = new UndefinedReference[operand1._references.length + operand2._references.length];
        int tx = 0;
        for (UndefinedReference ur : operand1._references) {
            temp[tx++] = ignoreReferenceFields ? ur.copy(FieldDescriptor.W) : ur;
        }
        for (UndefinedReference ur : operand2._references) {
            temp[tx++] = ignoreReferenceFields ? ur.copy(FieldDescriptor.W) : ur;
        }
        temp = UndefinedReference.coalesce(temp);

        return new IntegerValue(false, intrinsic, resultForm, temp);
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

        long newIntrinsicValue = operand1._value & operand2._value;
        return new IntegerValue(false, newIntrinsicValue, resultForm, refList.toArray(new UndefinedReference[0]));
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

        if (operand2._value == 0) {
            diagnostics.append(new TruncationDiagnostic(locale, "Division by zero"));
            throw new ExpressionException();
        }

        DivisionResult dres = new DivisionResult();
        long intQuotient = operand1._value / operand2._value;
        long intRemainder = operand1._value % operand2._value;
        long intCovered = intRemainder == 0 ? intQuotient : intQuotient + 1;
        dres._quotient = new IntegerValue(intQuotient);
        dres._remainder = new IntegerValue(intRemainder);
        dres._coveredQuotient = new IntegerValue(intCovered);

        return dres;
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

        long result = operand1._value * operand2._value;
        if (Math.abs(result) > 0_377777_77777L) {
            diagnostics.append(new TruncationDiagnostic(locale, "Result of multiplication too large"));
        }

        return new IntegerValue(operand1._value * operand2._value);
    }

    /**
     * Produces an IV which is the additive inverse of this one
     */
    public IntegerValue negate() {
        UndefinedReference[] negRefs = new UndefinedReference[_references.length];
        for (int ux = 0; ux < _references.length; ++ux) {
            negRefs[ux] = _references[ux].copy(!_references[ux]._isNegative);
        }

        return new IntegerValue(false, -_value, _form, negRefs);
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

        return new IntegerValue(false, (~_value) & 0_777777_777777L, _form, _references);
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

        long newIntrinsicValue = operand1._value | operand2._value;
        return new IntegerValue(false, newIntrinsicValue, resultForm, refList.toArray(new UndefinedReference[0]));
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

        long newIntrinsicValue = operand1._value ^ operand2._value;
        return new IntegerValue(false, newIntrinsicValue, resultForm, refList.toArray(new UndefinedReference[0]));
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
}
