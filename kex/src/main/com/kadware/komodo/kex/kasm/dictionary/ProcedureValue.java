/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.TextLine;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;

/**
 * A Value which represents a proc.
 */
public class ProcedureValue extends Value {

    public final TextLine[] _source;

    /**
     * constructor
     * @param flagged if this is flagged (probably it isn't)
     */
    private ProcedureValue(
        final boolean flagged,
        final TextLine[] source
    ) {
        super(flagged);
        _source = source;
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
     * Create a new copy of this object, with the given flagged value
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final boolean newFlagged
    ) throws TypeException {
        throw new TypeException();
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

    /**
     * Check for equality
     * @param obj comparison object
     * @return true if the objects are equal, else false
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        return false;
    }

    @Override public ValueType getType() { return ValueType.Procedure; }

    @Override
    public int hashCode(
    ) {
        int code = 0;
        for (TextLine textLine : _source) {
            code ^= textLine._text.hashCode();
        }
        return code;
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        return String.format("%s<proc>", _flagged ? "*" : "");
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        boolean _flagged = false;
        TextLine[] _value = null;

        public Builder setFlagged(boolean value)    { _flagged = value; return this; }
        public Builder setValue(TextLine[] value)   {_value = value; return this; }

        public ProcedureValue build(
        ) {
            if (_value == null) {
                throw new RuntimeException("Value not specified for IntegerValue builder");
            }

            return new ProcedureValue(_flagged, _value);
        }
    }
}
