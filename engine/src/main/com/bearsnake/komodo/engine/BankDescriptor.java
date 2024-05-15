/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;

/**
 * An 8-word Word36Array which describes a bank.
 * This structure is defined by the hardware PRM and exists in memory, although it may be presented from the outside
 * world for testing or setup purposes.
 */
public class BankDescriptor extends ArraySlice {

    /**
     * Constructor for a BankDescriptor which is located in a larger structure.
     * We do not alter the underlying array, in case it already contains a valid structure.
     * @param base base array
     * @param offset offset within the base array where the bank descriptor source code exists
     */
    public BankDescriptor(
        final ArraySlice base,
        final int offset
    ) {
        super(base, offset, 8);
    }

    /**
     * @return Access lock for this bank
     *          BD.Access_Lock:Word 0:H2
     */
    public AccessInfo getAccessLock() {
        int ring = (int)((get(0) & 0_600000) >> 16);
        short domain = (short)(get(0) & 0_177777);
        return new AccessInfo(ring, domain);
    }

    /**
     * @return ProcessorType of bank (has some impact on the validity/meaning of other fields
     *          BD.TYPE:Word 0 Bits 8-11
     */
    public BankType getBankType() {
        int code = ((int)(get(0) >> 24)) & 017;
        return BankType.get(code);
    }

    /**
     * @return Base Address - i.e., describes the logical physical address (if that makes sense) corresponding to the
     *          first word of this bank in memory.  The actual meaning of this depends upon the hardware architecture.
     *          For us, the UPI portion is held in bits 0-3 of Word 3, the segment in H2 of Word 2,
     *          and the offset is held in bits 4-35 of Word 3.
     *          Canonical architecture requires the offset to be an unsigned value, but hardware addressing had traditionally
     *          begun at an artificially high value (such as 0400000000000 or some such) -- which allowed the base value to be
     *          less than that (but still positive) in order to account for non-zero lower-limits.
     *          Consider a lower-limit of 01000: The relative address of 01000 is valid, and refers to the first word in the bank.
     *          But if the first word in the bank is base-address == (upi)0, referring to the first word in MSP storage,
     *          the address translation algorithm (base-address + relative-address) gives us an absolute address of 01000,
     *          which is wrong - we wanted 0.
     *          To fix this, the base-address must be shifted backward/downward by the lower-limit value.
     *          This is possible if base address begin at some high value.
     *          However, for us the base address begins at zero, so to shift it down by lower-limit, we need signed values here.
     */
    public AbsoluteAddress getBaseAddress() {
        return new AbsoluteAddress(this, 2);
    }

    /**
     * @return position of this bank relative to the first bank describing a large or very large bank
     *          BD.DISP:Word 4 Bits 3-17
     */
    public int getDisplacement() {
        return (int)(get(4) >> 18) & 077777;
    }

    /**
     * @return Execute, Read, and Write permissions for ring/domain at a lower level
     *          BD.GAP:Word 0 Bits 0-2
     */
    public AccessPermissions getGeneraAccessPermissions() {
        return new AccessPermissions((int) (get(0) >> 33));
    }

    /**
     * @return G-flag
     *          Addressing_Exception interrupt results if this BD is accessed by normal bank-manipulation handling
     *          BD.G:Word 0 Bit 13
     */
    public boolean getGeneralFault() {
        return (get(0) & 020_000000L) != 0;
    }

    /**
     * @return large bank flag:
     *          If false, this is a single bank no greater than 0_777777.
     *              BD.Lower_Limit has 512-word granularity and BD.Upper_Limit has 1-word granularity.
     *              This is required for BD.ProcessorType other than ExtendedMode.
     *          IF true, this is either a portion of a large bank no greater than 077_777777
     *              or of a very large bank no greater than 077777_777777.
     *              BD.Lower_Limit has 32768-word granularity, and BD.Upper_Limit has 64-word granularity.
     *          BD.S:Word 0 Bit 15
     */
    public boolean getLargeBank() {
        return (get(0) & 04_000000L) != 0;
    }

    /**
     * @return lower limit, subject to granularity specified in the large-bank field - for non-indirect banks
     *          Word 1 Bits 0-8
     */
    public int getLowerLimit() {
        return (int)(get(1) >> 27);
    }

    /**
     * @return lower limit with granularity normalized out (making this 1-word granularity)
     */
    public int getLowerLimitNormalized() {
        return getLargeBank() ? (getLowerLimit() << 15) : (getLowerLimit() << 9);
    }

    /**
     * @return Execute, Read, and Write permissions for ring/domain at a lower level
     *          BD.SAP:Word 0 Bits 3-5
     */
    public AccessPermissions getSpecialAccessPermissions() {
        return new AccessPermissions((int)(get(0) >> 30) & 07);
    }

    /**
     * @return Target bank L,BDI (only for indirect banks)
     *          L (level) is the top 3 bits in a the wrapped 18-bit field
     *          BDI is the remaining (bottom) 15 bits
     */
    public int getTargetLBDI() {
        return (int)(get(0) >> 18);
    }

    /**
     * @return upper limit, subject to granularity specified in the large-bank field - for non-indirect banks
     *          Word 1 Bits 9-35 (bottom 3 quarter words)
     */
    public int getUpperLimit() {
        return (int)(get(1) & 0777_777777L);
    }

    /**
     * @return upper limit with granularity normalized out (making this 1-word granularity)
     */
    public int getUpperLimitNormalized() {
        return getLargeBank() ? (getUpperLimit() << 6) : getUpperLimit();
    }

    /**
     * @return U flag (upper limit suppression control)
     *              If clear, the upper limit is taken from the bank descriptor for populating the
     *              bank register.  If set, the bank register's upper limit is 0777777 non-normalized.
     *              Only meaningful for large banks (S is set).
     *          BD.U:Word 0 Bit 16
     */
    public boolean getUpperLimitSuppressionControl() {
        return (get(0) & 02_000000L) != 0;
    }

    public void setAccessLock(
        final AccessInfo accessInfo
    ) {
        long result = get(0) & 0_777777_000000L;
        result |= accessInfo.get() & 0777777;
        set(0, result);
    }

    public void setBankType(
        final BankType value
    ) {
        long result = get(0) & 0_776077_777777L;
        result |= (long)(value._code) << 24;
        set(0, result);
    }

    public void setBaseAddress(
        final AbsoluteAddress baseAddress
    ) {
        baseAddress.populate(this, 2);
    }

    public void setGeneralAccessPermissions(
        final AccessPermissions value
    ) {
        long result = get(0) & 0_077777_777777L;
        result |= ((long)value.get() << 33);
        set(0, result);
    }

    public void setGeneralFault(
        final boolean value
    ) {
        long result = get(0) & 0_777757_777777L;
        if (value) {
            result |= 020_000000;
        }
        set(0, result);
    }

    public void setLargeBank(
        final boolean value
    ) {
        long result = get(0) & 0_777773_777777L;
        if (value) {
            result |= 04_000000;
        }
        set(0, result);
    }

    public void setLowerLimit(
        final int value
    ) {
        long result = get(1) & 0_000777_777777L;
        result |= (long)(value & 0777) << 27;
        set(1, result);
    }

    public void setLowerLimitNormalized(
        final int normalizedValue
    ) {
        if (getLargeBank()) {
            if ((normalizedValue & 077777) != 0) {
                throw new RuntimeException(String.format("Cannot normalize %d for large bank", normalizedValue));
            }
            setLowerLimit(normalizedValue >> 15);
        } else {
            if ((normalizedValue & 0777) != 0) {
                throw new RuntimeException(String.format("Cannot normalize %d for non-large bank", normalizedValue));
            }
            setLowerLimit(normalizedValue >> 9);
        }
    }

    public void setSpecialAccessPermissions(
        final AccessPermissions value
    ) {
        long result = get(0) & 0_707777_777777L;
        result |= ((long)value.get() << 30);
        set(0, result);
    }

    public void setUpperLimit(
        final int value
    ) {
        long result = get(1) & 0_777000_000000L;
        result |= value & 0777_777777L;
        set(1, result);
    }

    public void setUpperLimitNormalized(
        final int normalizedValue
    ) {
        if (getLargeBank()) {
            if ((normalizedValue & 077) != 0) {
                throw new RuntimeException(String.format("Cannot normlize %d for large bank", normalizedValue));
            }
            setUpperLimit(normalizedValue >> 6);
        } else {
            setUpperLimit(normalizedValue);
        }
    }

    public void setUpperLimitSuppressionControl(
        final boolean value
    ) {
        long result = get(0) & 0_777775_777777L;
        if (value) {
            result |= 02_000000;
        }
        set(0, result);
    }

    @Override
    public String toString() {
        return String.format("{type:%s lock:%s gap:%s sap:%s llim:%012o ulim:%012o addr:%s}",
                             getBankType().toString(),
                             getAccessLock().toString(),
                             getGeneraAccessPermissions().toString(),
                             getSpecialAccessPermissions().toString(),
                             getLowerLimitNormalized(),
                             getUpperLimitNormalized(),
                             getBaseAddress().toString());
    }
}
