/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.baselib;

/**
 * An extension of Word36 which describes a processor register.
 * Currently, there is no additional functionality nor attributes for a GeneralRegister over that of the base class.
 * Nevertheless, we'll leave this in place for the GeneralRegisterSet class, in case we think of something to add here, later.
 */
public class IndexRegister extends GeneralRegister {

    public static final long MASK_XI        = 0_777777_000000l;     //  standard increment value
    public static final long MASK_XI12      = 0_777700_000000l;     //  increment value for 24/12 exec index register
    public static final long MASK_XM        = 0_000000_777777l;     //  standard modifier value
    public static final long MASK_XM24      = 0_000077_777777l;     //  modifier value for 24/12 exec index register

    public static final long MASK_NOT_XI    = 0_000000_777777l;
    public static final long MASK_NOT_XI12  = 0_000077_777777l;
    public static final long MASK_NOT_XM    = 0_777777_000000l;
    public static final long MASK_NOT_XM24  = 0_777700_000000l;

    /**
     * Standard constructor
     */
    public IndexRegister(
    ) {
    }

    /**
     * Initial value constructor
     * <p>
     * @param value
     */
    public IndexRegister(
        final long value
    ) {
        super(value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getXI(
    ) {
        return getH1(_value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getXI12(
    ) {
        return getT1(_value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getXM(
    ) {
        return getH2(_value);
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getXM24(
    ) {
        return _value & 0_000077_777777l;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getSignedXI(
    ) {
        return getSignExtended18(getXI());
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getSignedXI12(
    ) {
        return getSignExtended12(getXI12());
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getSignedXM(
    ) {
        return getSignExtended18(getXM());
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public long getSignedXM24(
    ) {
        return getSignExtended24(getXM24());
    }

    /**
     * Setter
     * <p>
     * @param newValue
     */
    public void setXI(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XI) | ((newValue << 18) & MASK_XI);
    }

    /**
     * Setter
     * <p>
     * @param newValue
     */
    public void setXI12(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XI12) | ((newValue << 24) & MASK_XI12);
    }

    /**
     * Setter
     * <p>
     * @param newValue
     */
    public void setXM(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XM) | (newValue & MASK_XM);
    }

    /**
     * Setter
     * <p>
     * @param newValue
     */
    public void setXM24(
        final long newValue
    ) {
        _value = (_value & MASK_NOT_XM24) | (newValue & MASK_XM24);
    }

    /**
     * Decrements the 18-bit modifier portion by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public void decrementModifier18(
    ) {
        setXM(OnesComplement.add36Simple(getSignedXM(), OnesComplement.negate36(getSignedXI())));
    }

    /**
     * Increments the 18-bit modifier portion by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public void incrementModifier18(
    ) {
        setXM(OnesComplement.add36Simple(getSignedXM(), getSignedXI()));
    }

    /**
     * Increments the 24-bit modifier portion by the value in the 12-bit increment portion
     * using ones-complement arithmetic and assuming both the index and modifier portions are signed fields.
     */
    public void incrementModifier24(
    ) {
        setXM24(OnesComplement.add36Simple(getSignedXM24(), getSignedXI12()));
    }
}
