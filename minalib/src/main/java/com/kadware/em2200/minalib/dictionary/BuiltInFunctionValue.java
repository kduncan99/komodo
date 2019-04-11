/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * Class which represents a built-in-function name and related stuffs
 */
public class BuiltInFunctionValue extends Value {

    private final Class _clazz;

    /**
     * Normal constructor
     * <p>
     * @param clazz class of the function handler
     */
    public BuiltInFunctionValue(
        final Class clazz
    ) {
        super(false, Signed.None, Precision.None, null, null);
        _clazz = clazz;
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
     * <p>
     * @param newFlagged
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    @Override
    public BuiltInFunctionValue copy(
        final boolean newFlagged
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given signed value
     * <p>
     * @param newSigned
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    @Override
    public BuiltInFunctionValue copy(
        final Signed newSigned
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given precision value
     * <p>
     * @param newPrecision
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    @Override
    public BuiltInFunctionValue copy(
        final Precision newPrecision
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Check for equality
     * <p>
     * @param obj
     * <p>
     * @return
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (!(obj instanceof BuiltInFunctionValue)) {
            return false;
        }

        BuiltInFunctionValue bif = (BuiltInFunctionValue)obj;
        return (_clazz == bif._clazz);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.BuiltInFunction;
    }

    /**
     * Transform the value to an FloatingPointValue, if possible
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    @Override
    public StringValue toStringValue(
        final Locale locale,
        CharacterMode characterMode,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Class getClazz(
    ) {
        return _clazz;
    }
}
