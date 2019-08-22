/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.exceptions.TypeException;

/**
 * A Value which represents a form.
 */
public class FormValue extends Value {

    public final Form _form;

    /**
     * constructor
     * @param form form to be used
     */
    public FormValue(
        final Form form
    ) {
        super(false);
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
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final boolean newFlagged
    ) {
        return new FormValue(_form);
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
    @Override
    public ValueType getType(
    ) {
        return ValueType.Form;
    }

    /**
     * Getter
     * @return value
     */
    public Form getValue(
    ) {
        return _form;
    }

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
     * Transform the value to an IntegerValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new Value
     */
    @Override
    public FloatingPointValue toFloatingPointValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Transform the value to an IntegerValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new Value
     */
    @Override
    public IntegerValue toIntegerValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Transform the value to a StringValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode - we ignore this, as this applies only to conversions of something else
     * @param diagnostics where we post any necessary diagnostics
     * @return new Value
     */
    @Override
    public StringValue toStringValue(
        final Locale locale,
        final CharacterMode characterMode,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
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
