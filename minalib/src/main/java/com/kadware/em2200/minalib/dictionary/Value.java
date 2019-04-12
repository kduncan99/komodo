/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * Base class for a Value
 */
public abstract class Value {

    private final boolean _flagged;
    private final Form _form;
    private final Precision _precision;
    private final Signed _signed;
    private final RelocationInfo _relocatableInfo;

    /**
     * Normal constructor
     * @param flagged (i.e., has leading asterisk)
     * @param signed (i.e., has leading + or -)
     * @param precision (i.e., has trailing S or D)
     * @param form null if no form is attached
     * @param relocationInfo null if no relocation info is attached
     */
    protected Value(
        final boolean flagged,
        final Signed signed,
        final Precision precision,
        final Form form,
        final RelocationInfo relocationInfo
    ) {
        _flagged = flagged;
        _signed = signed;
        _precision = precision;
        _form = form;
        _relocatableInfo = relocationInfo;
    }

    /**
     * Appends attribute information to a string passed in a StringBuilder class.
     * This is part of the mechanism for producing user-readable dump of this object.
     * It is expected that the subclass which calls back here, has already deposited a
     * user-readable version of it's base value into the builder.
     * @param builder StringBuilder() object
     */
    protected void appendAttributes(
        final StringBuilder builder
    ) {
        if (_flagged) {
            builder.append(" Flagged");
        }

        if (_form != null) {
            builder.append(" F(");
            for (int fsx = 0; fsx < _form.getFieldCount(); ++fsx) {
                if (fsx > 0) {
                    builder.append(",");
                }
                builder.append(_form.getFieldSizes()[fsx]);
            }
            builder.append(")");
        }

        if (_signed == Signed.Negative) {
            builder.append(" Neg");
        } else if (_signed == Signed.Positive) {
            builder.append(" Pos");
        }

        if (_precision == Precision.Single) {
            builder.append(" Sgl");
        } else if (_precision == Precision.Double) {
            builder.append(" Dbl");
        }

        if (_relocatableInfo != null) {
            builder.append(_relocatableInfo.toString());
        }
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws RelocationException if the relocation info for this object and the comparison object are incompatible
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    public abstract int compareTo(
        final Object obj
    ) throws RelocationException,
             TypeException;

    /**
     * Create a new copy of this object, with the given flagged value
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public abstract Value copy(
        final boolean newFlagged
    ) throws TypeException;

    /**
     * Create a new copy of this object, with the given signed value
     * @param newSigned new value for Signed attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public abstract Value copy(
        final Signed newSigned
    ) throws TypeException;

    /**
     * Create a new copy of this object, with the given precision value
     * @param newPrecision new value for Precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    public abstract Value copy(
        final Precision newPrecision
    ) throws TypeException;

    /**
     * Check for equality
     * Must be overridden by subclass
     * @param obj comparison object
     * @return true if objects are equal, else false
     */
    @Override
    public abstract boolean equals(
        final Object obj
    );

    /**
     * Getter
     * @return value of Flagged attribute
     */
    public boolean getFlagged(
    ) {
        return _flagged;
    }

    /**
     * Getter
     * @return value of Form attribute or null
     */
    public Form getForm(
    ) {
        return _form;
    }

    /**
     * Getter
     * @return value of Precision attribute
     */
    public Precision getPrecision(
    ) {
        return _precision;
    }

    /**
     * Getter
     * @return value of RelocationInfo attribute or null
     */
    public RelocationInfo getRelocationInfo(
    ) {
        return _relocatableInfo;
    }

    public Signed getSigned(
    ) {
        return _signed;
    }

    /**
     * Getter
     * @return value type
     */
    public abstract ValueType getType();

    /**
     * Transform the value to an FloatPointValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new Value
     * @throws TypeException if object cannot be converted
     */
    public abstract FloatingPointValue toFloatingPointValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException;

    /**
     * Transform the value to an IntegerValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new Value
     * @throws TypeException if object cannot be converted
     */
    public abstract IntegerValue toIntegerValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException;

    /**
     * Transform the value to a StringValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
     * @param diagnostics where we post any necessary diagnostics
     * @return new Value
     * @throws TypeException if object cannot be converted
     */
    public abstract StringValue toStringValue(
        final Locale locale,
        CharacterMode characterMode,
        Diagnostics diagnostics
    ) throws TypeException;

    /**
     * Create user-readable string to describe this value.
     * Should be over-ridden by the various subclasses, and they must call back to appendAttributes()
     * @return user-readable string
     */
    public abstract String toString();
}
