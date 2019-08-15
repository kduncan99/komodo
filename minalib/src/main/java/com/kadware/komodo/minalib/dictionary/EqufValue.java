/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.UndefinedReference;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.diagnostics.RelocationDiagnostic;
import com.kadware.komodo.minalib.exceptions.TypeException;

import java.util.Arrays;

/**
 * A Value which represents a form.
 */
public class EqufValue extends IntegerValue {

    /**
     * constructor
     * @param flagged - leading asterisk
     * @param value - integer value
     * @param undefinedReferences - null if no undefined references are attached
     */
    public EqufValue(
        final boolean flagged,
        final long value,
        final UndefinedReference[] undefinedReferences
    ) {
        super(flagged, value, undefinedReferences);
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws TypeException if there is no reasonable way to compare the objects -
     *                          note that if the flagged and relocation info attributes are not equal,
     *                          no comparison can be done.
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws TypeException {
        if (obj instanceof EqufValue) {
            EqufValue eqobj = (EqufValue)obj;

            if ((eqobj._flagged == _flagged)
                && (eqobj._undefinedReferences.length == 0)
                && (_undefinedReferences.length == 0)) {
                return Long.compare(_value, eqobj._value);
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
        return new EqufValue(newFlagged, _value, _undefinedReferences);
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
        if (!(obj instanceof EqufValue)) {
            return false;
        }

        EqufValue eqobj = (EqufValue)obj;
        if (eqobj._flagged != _flagged) {
            return false;
        }

        //  This check isn't quite right, but it's close enough
        if ((eqobj._undefinedReferences != null) && (_undefinedReferences != null)) {
            if (!Arrays.equals(eqobj._undefinedReferences, _undefinedReferences)) {
                return false;
            }
        }

        return _value == eqobj._value;
    }

    /**
     * Getter
     * @return value type
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.Equf;
    }

    @Override
    public int hashCode() { return (int)(_value * 31); }

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
    ) {
        if (_undefinedReferences != null) {
            diagnostics.append(new RelocationDiagnostic(locale));
        }

        return new FloatingPointValue(_flagged, _value);
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
    ) {
        return this;
    }
}
