/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.exceptions.TypeException;

/**
 * A Value which represents a proc.
 */
public class ProcedureValue extends Value {

    public final TextLine[] _source;

    /**
     * constructor
     * @param flagged if this is flagged (probably it isn't)
     */
    public ProcedureValue(
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
    ) {
        return new ProcedureValue(newFlagged, _source);
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

    /**
     * Getter
     * @return value type
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.Procedure;
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
    public String toString(
    ) {
        return String.format("%s<proc>", _flagged ? "*" : "");
    }
}
