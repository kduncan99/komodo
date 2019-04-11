/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.baselib.Word36;
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
        private String _value;

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
     * <p>
     * @param flagged (leading asterisk)
     * @param signed
     * @param precision
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
     * <p>
     * @param newFlagged
     * <p>
     * @return
     */
    @Override
    public Value copy(
        final boolean newFlagged
    ) {
        return new StringValue(newFlagged, getSigned(), getPrecision(), _characterMode, _value);
    }

    /**
     * Create a new copy of this object, with the given signed value
     * <p>
     * @param newSigned
     * <p>
     * @return
     */
    @Override
    public Value copy(
        final Signed newSigned
    ) {
        return new StringValue(getFlagged(), newSigned, getPrecision(), _characterMode, _value);
    }

    /**
     * Create a new copy of this object, with the given precision value
     * <p>
     * @param newPrecision
     * <p>
     * @return
     */
    @Override
    public Value copy(
        final Precision newPrecision
    ) {
        return new StringValue(getFlagged(), getSigned(), newPrecision, _characterMode, _value);
    }

    /**
     * Creates a new copy of this object, with the given character mode
     * <p>
     * @param newMode
     * <p>
     * @return
     */
    public Value copy(
        final CharacterMode newMode
    ) {
        return new StringValue(getFlagged(), getSigned(), getPrecision(), newMode, _value);
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
        //????TODO Does not account for signed flag, which might okay for strings, though not compatible...
        //      if we DO care about it, then there are 2-word-max truncation issues to consider
        return (obj instanceof StringValue) && _value.equals(((StringValue)obj)._value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public CharacterMode getCharacterMode(
    ) {
        return _characterMode;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.String;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public String getValue(
    ) {
        return _value;
    }

    /**
     * Checks to see whether two words should be generated for this value, when it is interpreted as an integer.
     * Yes if Double precision, No if Single, and Yes if None and MSWord is non-zero.
     * <p>
     * @return
     */
    public boolean generateDoublePrecision(
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
     * <p>
     * @return
     */
    public boolean isPrecisionValid(
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics
     * <p>
     * @return
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
            result[0] = 0;
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics
     * <p>
     * @return
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
            result[0] = 0;
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode - we ignore this, as this applies only to conversions of something else
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     */
    @Override
    public StringValue toStringValue(
        final Locale locale,
        final CharacterMode characterMode,
        Diagnostics diagnostics
    ) {
        return this;
    }
}
