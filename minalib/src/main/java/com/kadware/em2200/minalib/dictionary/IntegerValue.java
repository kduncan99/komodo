/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.OnesComplement;
import com.kadware.em2200.baselib.Word36;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.exceptions.*;
import java.math.BigInteger;

/**
 * A Value which represents a 72-bit signed integer.
 * Note that this differs from the way MASM works.  Too bad.
 */
public class IntegerValue extends Value {

    public static class Builder {
        private boolean _flagged = false;
        private Form _form = null;
        private Precision _precision = Precision.None;
        private Signed _signed = Signed.None;
        private RelocationInfo _relocatableInfo = null;
        private long[] _value = new long[2];

        public Builder setFlagged(
            final boolean flagged
        ) {
            _flagged = flagged;
            return this;
        }

        public Builder setForm(
            final Form form
        ) {
            _form = form;
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

        public Builder setRelocationInfo(
            final RelocationInfo relocationInfo
        ) {
            _relocatableInfo = relocationInfo;
            return this;
        }

        public Builder setValue(
            final long value
        ) {
            _value[0] = (value >> 36);
            _value[1] = value & 0_777777_777777l;
            return this;
        }

        public Builder setValue(
            final long[] value
        ) {
            assert(value.length == 2);
            _value[0] = value[0];
            _value[1] = value[1];
            return this;
        }

        public IntegerValue build(
        ) {
            return new IntegerValue(_flagged, _signed, _precision, _form, _relocatableInfo, _value);
        }
    }

    // [0] is MS 36-bit Word, [1] is LS 36-bit Word
    private final long[] _value;

    /**
     * constructor
     * <p>
     * @param flagged - leading asterisk
     * @param signed - is this signed? pos or neg?
     * @param precision - only affects things at value generation time
     * @param form - null if no Form is attached
     * @param relInfo - null if no Relocation information is attached
     * @param value - two 36-bit values making up the 72-bit value
     */
    private IntegerValue(
        final boolean flagged,
        final Signed signed,
        final Precision precision,
        final Form form,
        final RelocationInfo relInfo,
        final long[] value
    ) {
        super(flagged, signed, precision, form, relInfo);
        _value = new long[2];
        _value[0] = value[0];
        _value[1] = value[1];
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
    @Override
    public int compareTo(
        final Object obj
    ) throws RelocationException,
             TypeException {
        if (obj instanceof IntegerValue) {
            IntegerValue iobj = (IntegerValue)obj;

            //  Account for relocation info
            RelocationInfo riThis = getRelocationInfo();
            RelocationInfo riThat = iobj.getRelocationInfo();
            if ((riThis == null) && (riThat == null)) {
                //  we're okay - null/null is compatible
            } else if ((riThis == null) || (riThat == null) || !riThis.equals(riThat)) {
                //  either one or the other (but not both) is null, or else they both exist, and are unequal
                throw new RelocationException();
            }

            //  Relocation is okay - do the comparison
            //???? WAIT - check signed, this impacts comparison
            return OnesComplement.compare72(_value, ((IntegerValue)obj)._value);
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
        return new IntegerValue(newFlagged, getSigned(), getPrecision(), getForm(), getRelocationInfo(), _value);
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
        return new IntegerValue(getFlagged(), newSigned, getPrecision(), getForm(), getRelocationInfo(), _value);
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
        return new IntegerValue(getFlagged(), getSigned(), newPrecision, getForm(), getRelocationInfo(), _value);
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
        if (!(obj instanceof IntegerValue)) {
            return false;
        }

        IntegerValue iobj = (IntegerValue)obj;

        //  Account for relocation info
        RelocationInfo riThis = getRelocationInfo();
        RelocationInfo riThat = iobj.getRelocationInfo();
        if ((riThis == null) && (riThat == null)) {
            //  we're okay - null/null is compatible
        } else if ((riThis == null) || (riThat == null) || !riThis.equals(riThat)) {
            //  either one or the other (but not both) is null, or else they both exist, and are unequal
            return false;
        }

        //  Result depends opon signed field.  None and Positive are effectively the same thing...
        Signed sThis = getSigned();
        Signed sThat = iobj.getSigned();
        if (((sThis == Signed.Negative) || (sThat == Signed.Negative)) && (sThis != sThat)) {
            //  one  or the other is negative, but not both.  Need to do some additional work.
            long[] negValue = new long[2];
            OnesComplement.copy72(iobj._value, negValue);
            return OnesComplement.isEqual72(_value, negValue);
        } else {
            //  Signs are effectively the same, so just do a simple comparison
            return OnesComplement.isEqual72(_value, iobj._value);
        }
    }

    /**
     * Checks to see whether two words should be generated for this value.
     * Yes if Double precision, No if Single, and Yes if None and MSWord is non-zero.
     * <p>
     * @return
     */
    public boolean generateDoublePrecision(
    ) {
        switch (getPrecision()) {
            case Double:    return true;
            case Single:    return false;
            case None:      return _value[0] != 0;
        }

        throw new InternalErrorRuntimeException("bad Precision value in IntegerValue.generateDoublePrecision()");
    }

    /**
     * Checks to see if the precision setting is valid.
     * None and Double are always valid.  Single is valid if the upper 36 bits are zero
     * <p>
     * @return
     */
    public boolean isPrecisionValid(
    ) {
        return !((getPrecision() == Precision.Single) && (_value[0] != 0));
    }

    /**
     * Getter
     * <p>
     * @return
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.Integer;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long[] getValue(
    ) {
        long[] result = new long[2];
        OnesComplement.copy72(_value, result);
        return result;
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
        //????TODO - Not sure how we're going to do this, but this *might* work
        BigInteger bi = OnesComplement.getNative72(_value);
        return new FloatingPointValue.Builder().setValue(bi.doubleValue())
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
        return this;
    }

    /**
     * Transform the value to a StringValue, if possible
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
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
        if (!isPrecisionValid()) {
            diagnostics.append(new TruncationDiagnostic(locale, "Value larger than precision"));
        }

        if (getRelocationInfo() != null) {
            diagnostics.append(new RelocationDiagnostic(locale));
        }

        String str;
        long msbits = 0_400400_400400l;
        if (generateDoublePrecision()) {
            if (characterMode == CharacterMode.ASCII) {
                if (((_value[0] & msbits) != 0) || ((_value[1] & msbits) != 0)) {
                    diagnostics.append(new TruncationDiagnostic(locale, "MSBits dropped for ASCII conversion"));
                }

                str = Word36.toASCII(_value[0]) + Word36.toASCII(_value[1]);
            } else {
                str = Word36.toFieldata(_value[0]) + Word36.toFieldata(_value[1]);
            }
        } else {
            if (characterMode == CharacterMode.ASCII) {
                if ((_value[1] & msbits) != 0) {
                    diagnostics.append(new TruncationDiagnostic(locale, "MSBits dropped for ASCII conversion"));
                }

                str = Word36.toASCII(_value[1]);
            } else {
                str = Word36.toFieldata(_value[1]);
            }
        }

        return new StringValue.Builder().setValue(str)
                                        .setCharacterMode(characterMode)
                                        .setPrecision(getPrecision())
                                        .build();
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString() {
        Word36 w0 = new Word36(_value[0]);
        Word36 w1 = new Word36(_value[1]);
        if (getSigned() == Signed.Negative) {
            w0.setW(Word36.logicalNot(w0.getW()));
            w1.setW(Word36.logicalNot(w1.getW()));
        }

        String str = String.format("%s%s", Word36.toOctal(w0.getW()), Word36.toOctal(w1.getW()));
        while (str.startsWith("0")) {
            str = str.substring(1);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("0");
        sb.append(str);
        super.appendAttributes(sb);
        return sb.toString();
    }
}
