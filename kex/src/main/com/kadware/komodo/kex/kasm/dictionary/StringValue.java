/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.CharacterMode;
import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;

/**
 * A Value which represents a string.
 * We do not do justification (all strings are left-justified) - it isn't worth the hassle...
 */
@SuppressWarnings("Duplicates")
public class StringValue extends Value {

    public final CharacterMode _characterMode;
    public final ValueJustification _justification;
    public final String _value;

    /**
     * constructor
     * @param locale where this is defined
     * @param flagged (leading asterisk)
     * @param value actual string content
     * @param characterMode ASCII or Fieldata
     * @param precision single/double/default
     * @param justification left/right/default
     */
    private StringValue(
        final Locale locale,
        final boolean flagged,
        final String value,
        final CharacterMode characterMode,
        final ValuePrecision precision,
        final ValueJustification justification
    ) {
        super(locale, flagged, precision);
        _characterMode = characterMode;
        _value = value;
        _justification = justification;
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
        if (obj instanceof StringValue) {
            StringValue sobj = (StringValue) obj;
            return _value.compareTo(sobj._value);
        } else {
            throw new TypeException();
        }
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param locale new value for Locale
     * @param newFlagged new value for Flagged attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final Locale locale,
        final boolean newFlagged
    ) {
        return new StringValue(locale, newFlagged, _value, _characterMode, _precision, _justification);
    }

    /**
     * Creates a new copy of this object, with the given justification
     * @param locale new value for Locale
     * @param newJustification new value for ValueJustification attribute
     * @return new Value
     */
    public Value copy(
        final Locale locale,
        final ValueJustification newJustification
    ) {
        return new StringValue(locale, _flagged, _value, _characterMode, _precision, newJustification);
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param locale new value for Locale
     * @param newPrecision new value for precision attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final Locale locale,
        final ValuePrecision newPrecision
    ) {
        return new StringValue(locale, _flagged, _value, _characterMode, newPrecision, _justification);
    }

    /**
     * Creates a new copy of this object, with the given character mode
     * @param locale new value for Locale
     * @param newMode new value for Mode attribute
     * @return new Value
     */
    public Value copy(
        final Locale locale,
        final CharacterMode newMode
    ) {
        return new StringValue(locale, _flagged, _value, newMode, _precision, _justification);
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
        if (obj instanceof StringValue) {
            StringValue sobj = (StringValue) obj;
            return ( sobj._flagged == _flagged ) && sobj._value.equals(_value);
        } else {
            return false;
        }
    }

    @Override public ValueType getType() { return ValueType.String; }
    @Override public int hashCode() { return _value.hashCode(); }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (_flagged) { sb.append("*"); }
        sb.append(_value);

        if (_justification == ValueJustification.Left) {
            sb.append("L");
        } else if (_justification == ValueJustification.Right) {
            sb.append("R");
        }

        if (_precision == ValuePrecision.Single) {
            sb.append("S");
        } else if (_precision == ValuePrecision.Double) {
            sb.append("D");
        }

        return sb.toString();
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Builder
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static class Builder {

        CharacterMode _characterMode = CharacterMode.Default;
        boolean _flagged = false;
        ValueJustification _justification = ValueJustification.Default;
        Locale _locale = null;
        ValuePrecision _precision = ValuePrecision.Default;
        String _value = null;

        public Builder setCharacterMode(CharacterMode value)        { _characterMode = value; return this; }
        public Builder setFlagged(boolean value)                    { _flagged = value; return this; }
        public Builder setJustification(ValueJustification value)   { _justification = value; return this; }
        public Builder setLocale(Locale value)                      { _locale = value; return this; }
        public Builder setPrecision(ValuePrecision value)           { _precision = value; return this; }
        public Builder setValue(String value)                       { _value = value; return this; }

        public StringValue build(
        ) {
            if (_value == null) {
                throw new RuntimeException("Value not specified for IntegerValue builder");
            }

            return new StringValue(_locale, _flagged, _value, _characterMode, _precision, _justification);
        }
    }
}
