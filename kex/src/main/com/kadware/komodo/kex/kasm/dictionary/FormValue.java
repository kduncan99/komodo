/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.Form;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;

/**
 * A Value which represents a form.
 */
public class FormValue extends Value {

    public final Form _form;

    /**
     * constructor
     * @param locale where this form was defined
     * @param form form to be used
     */
    public FormValue(
        final Locale locale,
        final Form form
    ) {
        super(locale);
        _form = form;
    }

    /**
     * constructor
     * @param form form to be used
     */
    public FormValue(
        final Form form
    ) {
        _form = form;
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
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given flagged value (since we ignore 'flagged', this simply does a copy)
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

    /**
     * Check for equality
     * @param obj comparison object
     * @return true if the objects are equal, else false
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof FormValue) {
            FormValue fvobj = (FormValue) obj;
            return fvobj._form.equals(_form);
        } else {
            return false;
        }
    }

    /**
     * Getter
     * @return value type
     */
    @Override public ValueType getType() { return ValueType.Form; }

    @Override
    public int hashCode()
    {
        int code = 0;
        for (int fieldSize : _form._fieldSizes) {
            code = (code << 1) + fieldSize;
        }
        return code;
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        return String.format("%s%s", _flagged ? "*" : "", _form.toString());
    }
}
