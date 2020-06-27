/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;

/**
 * A Value which represents a form along with some pre-built values associated with the form.
 * When this value is referenced, the form and the values are applied to the entire word
 * being generated.
 */
@SuppressWarnings("Duplicates")
public class EqufValue extends Value {

    public final Form _form;
    public final IntegerValue[] _values;

    /**
     * constructor
     * @param locale - where this was defined
     * @param values - integer values, with optional undefined references applied - one per form field
     * @param form - attached form
     */
    public EqufValue(
        final Locale locale,
        final IntegerValue[] values,
        final Form form
    ) {
        super(locale);
        _values = values;
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
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param locale new value for Locale
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final Locale locale,
        final boolean newFlagged
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param locale new value for Locale
     * @param newPrecision new value for precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    @Override
    public Value copy(
        final Locale locale,
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
            if (_values.length != ev._values.length) {
                return false;
            }

            for (int ax = 0; ax < _values.length; ++ax) {
                if (!_values[ax].equals(ev._values[ax])) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

    @Override public ValueType getType() { return ValueType.Equf; }

    @Override public int hashCode() {
        int result = _values.length << 30;
        for (IntegerValue value : _values) {
            result += value.hashCode();
        }
        return result;
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (_flagged) {
            sb.append("* ");
        }
        for (IntegerValue value : _values) {
            sb.append(String.format("%o ", value._value.get()));
        }
        return sb.toString();
    }

    /**
     * Arithmetically inverts all the values we're holding
     */
    public void invert(
        final Assembler assembler
    ) {
        for (int ivx = 0; ivx < _values.length; ++ivx) {
            _values[ivx] = _values[ivx].not(_values[ivx]._locale, assembler);
        }
    }
}
