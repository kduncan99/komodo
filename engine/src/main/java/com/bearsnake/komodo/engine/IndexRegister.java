/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.Word36;

/**
 * An extension of Word36 which describes a processor register.
 * Currently, there is no additional functionality nor attributes for a GeneralRegister over that of the base class.
 * Nevertheless, we'll leave this in place for the GeneralRegisterSet class, in case we think of something to add here, later.
 */
public class IndexRegister {

    public static final long MASK_XI        = 0_777777_000000L;     //  standard increment value
    public static final long MASK_XI12      = 0_777700_000000L;     //  increment value for 24/12 exec index register
    public static final long MASK_XM        = 0_000000_777777L;     //  standard modifier value
    public static final long MASK_XM24      = 0_000077_777777L;     //  modifier value for 24/12 exec index register

    public static final long MASK_NOT_XI    = 0_000000_777777L;
    public static final long MASK_NOT_XI12  = 0_000077_777777L;
    public static final long MASK_NOT_XM    = 0_777777_000000L;
    public static final long MASK_NOT_XM24  = 0_777700_000000L;

    private IndexRegister() {}

    public static long getXI(final long operand) { return Word36.getH1(operand); }
    public static long getXI12(final long operand) { return Word36.getT1(operand); }
    public static long getXM(final long operand) { return Word36.getH2(operand); }
    public static long getXM24(final long operand) { return operand & MASK_XM24; }
    public static long getSignedXI(final long operand) { return Word36.getSignExtended18(getXI(operand)); }
    public static long getSignedXI12(final long operand) { return Word36.getSignExtended12(getXI12(operand)); }
    public static long getSignedXM(final long operand) { return Word36.getSignExtended18(getXM(operand)); }
    public static long getSignedXM24(final long operand) { return Word36.getSignExtended24(getXM24(operand)); }

    public static long setXI(
        final long existingValue,
        final long newValue
    ) {
        return (existingValue & MASK_NOT_XI) | ((newValue << 18) & MASK_XI);
    }

    public static long setXI12(
        final long existingValue,
        final long newValue
    ) {
        return (existingValue & MASK_NOT_XI12) | ((newValue << 24) & MASK_XI12);
    }

    public static long setXM(
        final long existingValue,
        final long newValue
    ) {
        return (existingValue & MASK_NOT_XM) | (newValue & MASK_XM);
    }

    public static long setXM24(
        final long existingValue,
        final long newValue
    ) {
        return (existingValue & MASK_NOT_XM24) | (newValue & MASK_XM24);
    }

    /**
     * Decrements the 18-bit modifier portion by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public static long decrementModifier18(
        final long operand
    ) {
        return setXM(operand, Word36.addSimple(getSignedXM(operand), Word36.negate(getSignedXI(operand))));
    }

    /**
     * Increments the 18-bit modifier portion by the value in the 18-bit increment portion
     * using ones-complement arithmetic and assuming both the modifier and increment portions are signed fields.
     */
    public static long incrementModifier18(
        final long operand
    ) {
        return setXM(operand, Word36.addSimple(getSignedXM(operand), getSignedXI(operand)));
    }

    /**
     * Increments the 24-bit modifier portion by the value in the 12-bit increment portion
     * using ones-complement arithmetic and assuming both the index and modifier portions are signed fields.
     */
    public static long incrementModifier24(
        final long operand
    ) {
        return setXM24(operand, Word36.addSimple(getSignedXM24(operand), getSignedXI12(operand)));
    }
}
