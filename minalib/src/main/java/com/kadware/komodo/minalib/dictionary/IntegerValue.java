/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.minalib.*;
import com.kadware.komodo.minalib.diagnostics.*;
import com.kadware.komodo.minalib.exceptions.TypeException;

import java.util.Arrays;

/**
 * A Value which represents a 72-bit signed integer.
 * Note that this differs from the way MASM works.  Too bad.
 */
public class IntegerValue extends Value {

    public final UndefinedReference[] _undefinedReferences;     //  will never be null, but is often empty
    public final long _value;

    /**
     * constructor
     * @param flagged - leading asterisk
     * @param value - integer value
     * @param undefinedReferences - null if no undefined references are attached
     */
    public IntegerValue(
        final boolean flagged,
        final long value,
        final UndefinedReference[] undefinedReferences
    ) {
        super(flagged);
        _value = value;
        if (undefinedReferences != null) {
            _undefinedReferences = undefinedReferences;
        } else {
            _undefinedReferences = new UndefinedReference[0];
        }
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
        if (obj instanceof IntegerValue) {
            IntegerValue iobj = (IntegerValue)obj;

            if ((iobj._flagged == _flagged)
                && (iobj._undefinedReferences.length == 0)
                && (_undefinedReferences.length == 0)) {
                return Long.compare(_value, iobj._value);
            }
        }

        throw new TypeException();
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
        return new IntegerValue(newFlagged, _value, _undefinedReferences);
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
        if (!(obj instanceof IntegerValue)) {
            return false;
        }

        IntegerValue iobj = (IntegerValue)obj;
        if (iobj._flagged != _flagged) {
            return false;
        }

        //  This check isn't quite right, but it's close enough
        if ((iobj._undefinedReferences != null) && (_undefinedReferences != null)) {
            if (!Arrays.equals(iobj._undefinedReferences, _undefinedReferences)) {
                return false;
            }
        }

        return _value == iobj._value;
    }

    /**
     * Generate hash code
     */
    @Override
    public int hashCode() {
        return (int)(_value * 31);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.Integer;
    }

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
        if (_undefinedReferences != null) {
            diagnostics.append(new RelocationDiagnostic(locale));
        }

        return new FloatingPointValue(_flagged, _value);
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
        return this;
    }

    /**
     * Transform the value to a StringValue, if possible
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
        if (_undefinedReferences.length > 0) {
            diagnostics.append(new RelocationDiagnostic(locale));
        }

        String str;
        long msbits = 0_400400_400400L;
        if (characterMode == CharacterMode.ASCII) {
            if ((_value & msbits) != 0) {
                diagnostics.append(new TruncationDiagnostic(locale, "MSBits dropped for ASCII conversion"));
            }

            str = Word36.toASCII(_value);
        } else {
            str = Word36.toFieldata(_value);
        }

        return new StringValue(_flagged, str, characterMode);
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s%012o",
                                _flagged ? "*" : "",
                                _value));
        for (UndefinedReference ur : _undefinedReferences) {
            sb.append(ur.toString());
        }
        return sb.toString();
    }
}
