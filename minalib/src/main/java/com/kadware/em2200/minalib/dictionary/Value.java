/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
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
     * Default constructor
     */
    public Value(
    ) {
        _flagged = false;
        _signed = Signed.None;
        _form = null;
        _precision = Precision.None;
        _relocatableInfo = null;
    }

    /**
     * Flagging constructor
     * <p>
     * @param flagged
     */
    public Value(
        final boolean flagged
    ) {
        _flagged = flagged;
        _signed = Signed.None;
        _form = null;
        _precision = Precision.None;
        _relocatableInfo = null;
    }

    /**
     * Normal constructor
     * <p>
     * @param flagged (i.e., has leading asterisk)
     * @param signed (i.e., has leading + or -)
     * @param precision (i.e., has trailing S or D)
     * @param form null if no form is attached
     * @param relocationInfo null if no relocation info is attached
     */
    public Value(
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
        _relocatableInfo = null;
    }

    /**
     * Compares an object to this object
     * <p>
     * @param obj
     * <p>
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * <p>
     * @throws RelocationException if the relocation info for this object and the comparison object are incompatible
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    public abstract int compareTo(
        final Object obj
    ) throws RelocationException,
             TypeException;

    /**
     * Create a new copy of this object, with the given flagged value
     * <p>
     * @param newFlagged
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    public abstract Value copy(
        final boolean newFlagged
    ) throws TypeException;

    /**
     * Create a new copy of this object, with the given signed value
     * <p>
     * @param newSigned
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    public abstract Value copy(
        final Signed newSigned
    ) throws TypeException;

    /**
     * Create a new copy of this object, with the given precision value
     * <p>
     * @param newPrecision
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    public abstract Value copy(
        final Precision newPrecision
    ) throws TypeException;

    /**
     * Check for equality
     * Must be overridden by subclass
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public abstract boolean equals(
        final Object obj
    );

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getFlagged(
    ) {
        return _flagged;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Form getForm(
    ) {
        return _form;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Precision getPrecision(
    ) {
        return _precision;
    }

    /**
     * Getter
     * <p>
     * @return
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
     * <p>
     * @return
     */
    public abstract ValueType getType();

    /**
     * Transform the value to an FloatPointValue, if possible
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    public abstract FloatingPointValue toFloatingPointValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException;

    /**
     * Transform the value to an IntegerValue, if possible
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    public abstract IntegerValue toIntegerValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException;

    /**
     * Transform the value to a StringValue, if possible
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    public abstract StringValue toStringValue(
        final Locale locale,
        CharacterMode characterMode,
        Diagnostics diagnostics
    ) throws TypeException;
}
