/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

public class Register extends Word36 {

    protected long _value;

    protected Register() {}

    public long getValue() { return _value; }
    public void setValue(final long value) { _value = value; }

    public static final long MASK_XI        = 0_777777_000000L;     //  standard increment value
    public static final long MASK_XI12      = 0_777700_000000L;     //  increment value for 24/12 exec index register
    public static final long MASK_XM        = 0_000000_777777L;     //  standard modifier value
    public static final long MASK_XM24      = 0_000077_777777L;     //  modifier value for 24/12 exec index register

    public static final long MASK_NOT_XI    = 0_000000_777777L;
    public static final long MASK_NOT_XI12  = 0_000077_777777L;
    public static final long MASK_NOT_XM    = 0_777777_000000L;
    public static final long MASK_NOT_XM24  = 0_777700_000000L;

    public long getW() { return _value; }

    public long getXI() { return (_value >> 18) & 0_777777L; }
    public long getXI12() { return (_value >> 24) & 0_7777L; }
    public long getXM() { return _value & 0_777777L; }
    public long getXM24() { return _value & 0_7777_7777L; }

    public long getSignedXI() { return getSignExtended18(getXI()); }
    public long getSignedXI12() { return getSignExtended12(getXI12()); }
    public long getSignedXM() { return getSignExtended18(getXM()); }
    public long getSignedXM24() { return getSignExtended24(getXM24()); }

    public Register setXI(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XI) | ((newValue << 18) & MASK_XI);
        return this;
    }

    public Register setXI12(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XI12) | ((newValue << 24) & MASK_XI12);
        return this;
    }

    public Register setXM(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XM) | (newValue & MASK_XM);
        return this;
    }

    public Register setXM24(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XM24) | (newValue & MASK_XM24);
        return this;
    }

    /**
     * Decrements the 18-bit unsigned counter portion of an R register.
     */
    public Register decrementCounter18() {
        _value = (--_value) & 0_777777;
        return this;
    }

    /**
     * Decrements the 24-bit unsigned counter portion of an R register.
     */
    public Register decrementCounter24() {
        _value = (--_value) & 0_7777_7777;
        return this;
    }

    /**
     * Decrements the 18-bit modifier portion of an X register by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public Register decrementModifier18() {
        return setXM(Word36.addSimple(getSignedXM(), Word36.negate(getSignedXI())));
    }

    /**
     * Increments the 18-bit modifier portion of an X register by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public Register incrementModifier18() {
        return setXM(Word36.addSimple(getSignedXM(), getSignedXI()));
    }

    /**
     * Increments the 24-bit modifier portion of an X register by the value in the 12-bit increment portion
     * using ones-complement arithmetic and assuming both the index and modifier portions are signed fields.
     */
    public Register incrementModifier24() {
        return setXM24(Word36.addSimple(getSignedXM24(), getSignedXI12()));
    }
}
