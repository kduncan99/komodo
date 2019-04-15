/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.math.BigInteger;

/**
 * A Value which represents a string.
 * We do not do justification (all strings are left-justified) - it isn't worth the hassle...
 */
public class StringValue extends Value {

    public static class Builder {
        private CharacterMode _characterMode = CharacterMode.ASCII;
        private boolean _flagged = false;
        private Precision _precision = Precision.None;
        private Signed _signed = Signed.None;
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

        public Builder setPrecision(
            final Precision precision
        ) {
            _precision = precision;
            return this;
        }

        public Builder setSigned(
            final Signed signed
        ) {
            _signed = signed;
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
            return new StringValue(_flagged, _signed, _precision, _characterMode, _value);
        }
    }

    private final CharacterMode _characterMode;
    private final String _value;

    /**
     * constructor
     * @param flagged (leading asterisk)
     * @param signed Signed value
     * @param precision Precision value
     * @param characterMode ASCII or Fieldata
     * @param value actual string content
     */
    private StringValue(
        final boolean flagged,
        final Signed signed,
        final Precision precision,
        final CharacterMode characterMode,
        final String value
    ) {
        super(flagged, signed, precision, null, null);
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
        //????TODO Does not account for signed flag, which might okay for strings, though not compatible...
        //      if we DO care about it, then there are 2-word-max truncation issues to consider
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
        return new StringValue(newFlagged, getSigned(), getPrecision(), _characterMode, _value);
    }

    /**
     * Create a new copy of this object, with the given signed value
     * @param newSigned new value for Signed attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final Signed newSigned
    ) {
        return new StringValue(getFlagged(), newSigned, getPrecision(), _characterMode, _value);
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param newPrecision new value for Precision attribute
     * @return new Value
     */
    @Override
    public Value copy(
        final Precision newPrecision
    ) {
        return new StringValue(getFlagged(), getSigned(), newPrecision, _characterMode, _value);
    }

    /**
     * Creates a new copy of this object, with the given character mode
     * @param newMode new value for Mode attribute
     * @return new Value
     */
    public Value copy(
        final CharacterMode newMode
    ) {
        return new StringValue(getFlagged(), getSigned(), getPrecision(), newMode, _value);
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
        //????TODO Does not account for signed flag, which might okay for strings, though not compatible...
        //      if we DO care about it, then there are 2-word-max truncation issues to consider
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
     * Checks to see whether two words should be generated for this value, when it is interpreted as an integer.
     * Yes if Double precision, No if Single, and Yes if None and MSWord is non-zero.
     * @return true if two words should be generated, else false
     */
    private boolean generateDoublePrecision(
    ) {
        switch (getPrecision()) {
            case Double:    return true;
            case Single:    return false;
            case None:
                int bytesPerWord = _characterMode == CharacterMode.ASCII ? 4 : 6;
                return _value.length() > bytesPerWord;
        }

        throw new InternalErrorRuntimeException("bad Precision value in IntegerValue.generateDoublePrecision()");
    }

    /**
     * Checks to see if the precision setting is valid, given the length of the string value
     * and the character mode.
     * @return true if precision setting is valid for the string value and character mode, else false
     */
    private boolean isPrecisionValid(
    ) {
        int bytesPerWord = _characterMode == CharacterMode.ASCII ? 4 : 6;
        switch (getPrecision()) {
            case None:      return true;
            case Single:    return _value.length() <= bytesPerWord;
            case Double:    return _value.length() <= (2 * bytesPerWord);
        }

        throw new InternalErrorRuntimeException("bad Precision value in IntegerValue.generateDoublePrecision()");
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
        if (!isPrecisionValid()) {
            diagnostics.append(new TruncationDiagnostic(locale, "Value larger than precision"));
        }

        int bytesPerWord = _characterMode == CharacterMode.ASCII ? 4 : 6;
        long[] result = new long[2];
        if (generateDoublePrecision()) {
            String s2 = _value.length() <= bytesPerWord ? "" : _value.substring(bytesPerWord, 2 * bytesPerWord);
            if (_characterMode == CharacterMode.ASCII) {
                result[0] = Word36.stringToWord36ASCII(_value).getW();
                result[1] = Word36.stringToWord36ASCII(s2).getW();
            } else {
                result[0] = Word36.stringToWord36Fieldata(_value).getW();
                result[1] = Word36.stringToWord36Fieldata(s2).getW();
            }
        } else {
            if (_characterMode == CharacterMode.ASCII) {
                result[1] = Word36.stringToWord36ASCII(_value).getW();
            } else {
                result[1] = Word36.stringToWord36Fieldata(_value).getW();
            }
        }

        BigInteger bi = OnesComplement.getNative72(result);
        return new FloatingPointValue.Builder().setValue(bi.doubleValue())
                                               .setPrecision(getPrecision())
                                               .build();
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
        if (!isPrecisionValid()) {
            diagnostics.append(new TruncationDiagnostic(locale, "Value larger than precision"));
        }

        int bytesPerWord = _characterMode == CharacterMode.ASCII ? 4 : 6;
        long[] result = new long[2];
        if (generateDoublePrecision()) {
            String s2 = _value.length() <= bytesPerWord ? "" : _value.substring(bytesPerWord, 2 * bytesPerWord);
            if (_characterMode == CharacterMode.ASCII) {
                result[0] = Word36.stringToWord36ASCII(_value).getW();
                result[1] = Word36.stringToWord36ASCII(s2).getW();
            } else {
                result[0] = Word36.stringToWord36Fieldata(_value).getW();
                result[1] = Word36.stringToWord36Fieldata(s2).getW();
            }
        } else {
            if (_characterMode == CharacterMode.ASCII) {
                result[1] = Word36.stringToWord36ASCII(_value).getW();
            } else {
                result[1] = Word36.stringToWord36Fieldata(_value).getW();
            }
        }

        return new IntegerValue.Builder().setValue(result)
                                         .setPrecision(getPrecision())
                                         .build();
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
