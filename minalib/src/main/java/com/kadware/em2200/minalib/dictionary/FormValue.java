/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;

/**
 * A Value which represents a string.
 * We do not do justification (all strings are left-justified) - it isn't worth the hassle...
 */
public class StringValue extends Value {

    public static class Builder {
        private CharacterMode _characterMode = CharacterMode.ASCII;
        private boolean _flagged = false;
        private String _value = null;

        public Builder setCharacterMode(
            final CharacterMode characterMode
        ) {
            _characterMode = characterMode;
            return this;
        }

        public Builder setFlagged(
            final boolean flagged
        ) {
            _flagged = flagged;
            return this;
        }

        public Builder setValue(
            final String value
        ) {
            _value = value;
            return this;
        }

        public StringValue build(
        ) {
            return new StringValue(_flagged, _characterMode, _value);
        }
    }

    private final CharacterMode _characterMode;
    private final String _value;

    /**
     * constructor
     * @param flagged (leading asterisk)
     * @param characterMode ASCII or Fieldata
     * @param value actual string content
     */
    private StringValue(
        final boolean flagged,
        final CharacterMode characterMode,
        final String value
    ) {
        super(flagged, null);
        _characterMode = characterMode;
        _value = value;
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
            return _value.compareTo(((StringValue)obj)._value);
        } else {
            throw new TypeException();
        }
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
        return new StringValue(newFlagged, _characterMode, _value);
    }

    /**
     * Creates a new copy of this object, with the given character mode
     * @param newMode new value for Mode attribute
     * @return new Value
     */
    public Value copy(
        final CharacterMode newMode
    ) {
        return new StringValue(getFlagged(), newMode, _value);
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
        return (obj instanceof StringValue) && _value.equals(((StringValue)obj)._value);
    }

    /**
     * Getter
     * @return character mode attribute
     */
    public CharacterMode getCharacterMode(
    ) {
        return _characterMode;
    }

    /**
     * Getter
     * @return value type
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.String;
    }

    /**
     * Getter
     * @return value
     */
    public String getValue(
    ) {
        return _value;
    }

    /**
     * Transform the value to an FloatingPointValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new Value
     */
    @Override
    public FloatingPointValue toFloatingPointValue(
        final Locale locale,
        Diagnostics diagnostics
    ) {
        if (_characterMode == CharacterMode.ASCII) {
            return new FloatingPointValue.Builder().setValue(Word36.stringToWord36ASCII(_value).getW()).build();
        } else {
            return new FloatingPointValue.Builder().setValue(Word36.stringToWord36Fieldata(_value).getW()).build();
        }
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
        if (_characterMode == CharacterMode.ASCII) {
            return new IntegerValue.Builder().setValue(Word36.stringToWord36ASCII(_value).getW()).build();
        } else {
            return new IntegerValue.Builder().setValue(Word36.stringToWord36Fieldata(_value).getW()).build();
        }
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
    ) {
        return this;
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_value);
        super.appendAttributes(sb);
        return sb.toString();
    }
}
