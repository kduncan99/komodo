/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.baselib;

/**
 * An extension of Word36 which describes a processor register.
 * Currently, there is no additional functionality nor attributes for a GeneralRegister over that of the base class.
 * Nevertheless, we'll leave this in place for the GeneralRegisterSet class, in case we think of something to add here, later.
 */
public class IndexRegister extends GeneralRegister {

    public static final long MASK_XI        = 0_777777_000000L;     //  standard increment value
    public static final long MASK_XI12      = 0_777700_000000L;     //  increment value for 24/12 exec index register
    public static final long MASK_XM        = 0_000000_777777L;     //  standard modifier value
    public static final long MASK_XM24      = 0_000077_777777L;     //  modifier value for 24/12 exec index register

    public static final long MASK_NOT_XI    = 0_000000_777777L;
    public static final long MASK_NOT_XI12  = 0_000077_777777L;
    public static final long MASK_NOT_XM    = 0_777777_000000L;
    public static final long MASK_NOT_XM24  = 0_777700_000000L;

    public IndexRegister() {}
    public IndexRegister(final long value) { super(value); }

    public long getXI()         { return getH1(); }
    public long getXI12()       { return getT1(); }
    public long getXM()         { return getH2(); }
    public long getXM24()       { return _value & 0_000077_777777L; }
    public long getSignedXI()   { return getSignExtended18(getXI()); }
    public long getSignedXI12() { return getSignExtended12(getXI12()); }
    public long getSignedXM()   { return getSignExtended18(getXM()); }
    public long getSignedXM24() { return getSignExtended24(getXM24()); }

    public void setXI(long newValue)    { _value = (_value & MASK_NOT_XI) | ((newValue << 18) & MASK_XI); }
    public void setXI12(long newValue)  { _value = (_value & MASK_NOT_XI12) | ((newValue << 24) & MASK_XI12); }
    public void setXM(long newValue)    { _value = (_value & MASK_NOT_XM) | (newValue & MASK_XM); }
    public void setXM24(long newValue)  { _value = (_value & MASK_NOT_XM24) | (newValue & MASK_XM24); }

    /**
     * Decrements the 18-bit modifier portion by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public void decrementModifier18() {
        setXM(addSimple(getSignedXM(), negate(getSignedXI())));
    }

    /**
     * Increments the 18-bit modifier portion by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public void incrementModifier18() {
        setXM(addSimple(getSignedXM(), getSignedXI()));
    }

    /**
     * Increments the 24-bit modifier portion by the value in the 12-bit increment portion
     * using ones-complement arithmetic and assuming both the index and modifier portions are signed fields.
     */
    public void incrementModifier24() {
        setXM24(addSimple(getSignedXM24(), getSignedXI12()));
    }
}
