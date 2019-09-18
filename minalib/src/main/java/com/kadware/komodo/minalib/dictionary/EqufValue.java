/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.minalib.Form;
import com.kadware.komodo.minalib.UndefinedReference;
import com.kadware.komodo.minalib.exceptions.FormException;
import com.kadware.komodo.minalib.exceptions.RelocationException;
import com.kadware.komodo.minalib.exceptions.TypeException;
import java.util.Arrays;

/**
 * A Value which represents a form along with some pre-built values associated with the form.
 * When this value is referenced, the form and the values are applied to the entire word
 * being generated.
 */
@SuppressWarnings("Duplicates")
public class EqufValue extends Value {

    public final Form _form;
    public final IntegerValue _value;

    /**
     * constructor
     * @param flagged - leading asterisk
     * @param value - integer value, with optional undefined references applied
     * @param form - attached form (can be, but shouldn't be null)
     */
    private EqufValue(
        final boolean flagged,
        final IntegerValue value,
        final Form form
    ) {
        super(flagged);
        _value = value;
        _form = form;
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws FormException,
             TypeException,
             RelocationException {
        if (obj instanceof EqufValue) {
            EqufValue ev = (EqufValue) obj;
            if (ev._form.equals(_form)) {
                return ev._value.compareTo(ev._value);
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
        return new EqufValue(newFlagged, _value, _form);
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param newPrecision new value for precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    @Override
    public Value copy(
        final ValuePrecision newPrecision
    ) throws TypeException {
        throw new TypeException();
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof EqufValue) {
            EqufValue ev = (EqufValue) obj;
            return _value.equals(ev._value) && _form.equals(ev._form);
        }

        return false;
    }

    @Override public ValueType getType() { return ValueType.Equf; }
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

        return sb.toString();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        boolean _flagged = false;
        Form _form = null;
        IntegerValue _value = null;

        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }
        public Builder setForm(Form value)                          {_form = value; return this; }
        public Builder setValue(IntegerValue value)                 { _value = value; return this; }

        public EqufValue build(
        ) {
            if (_value == null) {
                throw new RuntimeException("Value not specified for EqufValue builder");
            }

            return new EqufValue(_flagged, _value, _form);
        }
    }
}
