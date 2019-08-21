/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.minalib.CharacterMode;
import com.kadware.komodo.minalib.Form;
import com.kadware.komodo.minalib.Locale;
import com.kadware.komodo.minalib.UndefinedReference;
import com.kadware.komodo.minalib.diagnostics.Diagnostics;
import com.kadware.komodo.minalib.exceptions.TypeException;

/**
 * A Value which represents a form.
 */
public class EqufValue extends IntegerValue {

    /**
     * constructor
     * @param flagged - leading asterisk
     * @param value - integer value
     * @param form - attached form (can be, but shouldn't be null)
     * @param undefinedReferences - null if no undefined references are attached
     */
    public EqufValue(
        final boolean flagged,
        final long value,
        final Form form,
        final UndefinedReference[] undefinedReferences
    ) {
        super(flagged, value, form, undefinedReferences);
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

    @Override
    public StringValue toStringValue(
        final Locale locale,
        CharacterMode characterMode,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
    }
}
