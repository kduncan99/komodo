/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.dictionary;

import com.kadware.komodo.minalib.exceptions.TypeException;

/**
 * Class which represents a directive
 */
public class DirectiveValue extends Value {

    public final Class _class;

    /**
     * Normal constructor
     * @param clazz class of the function handler
     */
    private DirectiveValue(Class clazz) { _class = clazz; }

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
     * @param newFlagged new attribute value
     * @return new value
     * @throws TypeException always - cannot be copied
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
     * @return true if objects are equal
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (!(obj instanceof DirectiveValue)) {
            return false;
        }

        DirectiveValue dir = (DirectiveValue)obj;
        return (_class.equals(dir._class));
    }

    @Override public ValueType getType() { return ValueType.Directive; }
    @Override public int hashCode() { return _class.hashCode(); }

//    /**
//     * Transform the value to an FloatingPointValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new value
//     * @throws TypeException since we cannot actually do this
//     */
//    @Override
//    public FloatingPointValue toFloatingPointValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) throws TypeException {
//        throw new TypeException();
//    }
//
//    /**
//     * Transform the value to an IntegerValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new value
//     * @throws TypeException since we cannot actually do this
//     */
//    @Override
//    public IntegerValue toIntegerValue(
//        final Locale locale,
//        Diagnostics diagnostics
//    ) throws TypeException {
//        throw new TypeException();
//    }
//
//    /**
//     * Transform the value to a StringValue, if possible
//     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
//     * @param characterMode desired character mode
//     * @param diagnostics where we post any necessary diagnostics
//     * @return new value
//     * @throws TypeException since we cannot actually do this
//     */
//    @Override
//    public StringValue toStringValue(
//        final Locale locale,
//        CharacterMode characterMode,
//        Diagnostics diagnostics
//    ) throws TypeException {
//        throw new TypeException();
//    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        return "<directive>";
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        Class _class;
        boolean _flagged = false;

        public Builder setClass(Class value)                        { _class = value; return this; }
        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }

        public DirectiveValue build(
        ) {
            if (_class == null) {
                throw new RuntimeException("Class not specified for DirectiveValue builder");
            }

            return new DirectiveValue(_class);
        }
    }
}
