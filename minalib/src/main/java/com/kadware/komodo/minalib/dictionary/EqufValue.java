/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.minalib.Form;
import com.kadware.komodo.minalib.UndefinedReference;
import com.kadware.komodo.minalib.exceptions.TypeException;
import java.util.Arrays;

/**
 * A Value which represents a form.
 */
@SuppressWarnings("Duplicates")
public class EqufValue extends Value {

    public final Form _form;
    public final UndefinedReference[] _references;
    public final Word36 _value;

    /**
     * constructor
     * @param flagged - leading asterisk
     * @param value - integer value
     * @param form - attached form (can be, but shouldn't be null)
     * @param undefinedReferences - null if no undefined references are attached
     */
    private EqufValue(
        final boolean flagged,
        final Word36 value,
        final Form form,
        final UndefinedReference[] undefinedReferences
    ) {
        super(flagged);
        _value = value;
        _form = form;
        _references = undefinedReferences;
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws TypeException {
        if (obj instanceof EqufValue) {
            EqufValue ev = (EqufValue) obj;
            if (ev._form.equals(_form)) {
                return ev._value.compare(ev._value);
            }
        }

        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final boolean newFlagged
    ) {
        return new EqufValue(newFlagged, _value, _form, _references);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof EqufValue) {
            EqufValue ev = (EqufValue) obj;
            return (_value.equals(ev._value))
                   && (_form.equals(ev._form)
                       && UndefinedReference.equals(_references, ev._references));
        }

        return false;
    }

    @Override public ValueType getType() { return ValueType.Equf; }
    @Override public int hashCode() { return _value.hashCode(); }

//    @Override
//    public FloatingPointValue toFloatingPointValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) throws TypeException {
//        throw new TypeException();
//    }
//
//    /**
//     * Transform the value to an IntegerValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new Value
//     */
//    @Override
//    public IntegerValue toIntegerValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) throws TypeException {
//        throw new TypeException();
//    }
//
//    @Override
//    public StringValue toStringValue(
//        final Locale locale,
//        CharacterMode characterMode,
//        Diagnostics diagnostics
//    ) throws TypeException {
//        throw new TypeException();
//    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%s%012o",
                                _flagged ? "*" : "",
                                _form == null ? "" : _form.toString(),
                                _value.getW()));
        for (UndefinedReference ur : _references) {
            sb.append(ur.toString());
        }

        return sb.toString();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        boolean _flagged = false;
        Form _form = null;
        UndefinedReference[] _references = new UndefinedReference[0];
        Word36 _value = null;

        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }
        public Builder setForm(Form value)                          {_form = value; return this; }
        public Builder setReferences(UndefinedReference[] values)   { _references = Arrays.copyOf(values, values.length); return this; }
        public Builder setValue(Word36 value)                       { _value = value; return this; }
        public Builder setValue(long value)                         { _value = new Word36(value); return this; }

        public EqufValue build(
        ) {
            if (_value == null) {
                throw new RuntimeException("Value not specified for EqufValue builder");
            }

            return new EqufValue(_flagged, _value, _form, _references);
        }
    }
}
