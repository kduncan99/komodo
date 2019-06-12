/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.AccessInfo;
import com.kadware.em2200.baselib.AccessPermissions;
import com.kadware.em2200.baselib.ArraySlice;
import com.kadware.em2200.baselib.exceptions.*;

/**
 * An 8-word Word36Array which describes a bank.
 * This structure is defined by the hardware PRM and exists in memory, although it may be presented from the outside
 * world for testing or setup purposes.
 */
public class BankDescriptor extends ArraySlice {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates the type of the bank
     */
    public enum BankType {
        ExtendedMode(0),
        BasicMode(1),     //  Requires BD.S == 0
        Gate(2),          //  Requires BD.S == 0
        Indirect(3),      //  Word1:H1 contains L,BDI of the target bank
                                //  Only BD.Type, BD.Disp, BD.G, and BD.L are valid, Requires BD.S == 0
        Queue(4),
        QueueRepository(6);

        private final int _code;

        BankType(
            final int code
        ) {
            _code = code;
        }

        public static BankType get(
            final int code
        ) {
            switch (code) {
                case 0:     return ExtendedMode;
                case 1:     return BasicMode;
                case 2:     return Gate;
                case 3:     return Indirect;
                case 4:     return Queue;
                case 6:     return QueueRepository;
            }

            throw new InvalidArgumentRuntimeException(String.format("Bad code passed to BankType.get:%d", code));
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard Constructor
     */
    public BankDescriptor(
    ) {
        super(new long[9]);
    }

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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Special Getter
     * @return Access lock for this bank
     *          BD.Access_Lock:Word 0:H2
     */
    public AccessInfo getAccessLock(
    ) {
        byte ring = (byte)((get(0) & 0_600000) >> 16);
        short domain = (short)(get(0) & 0_177777);
        return new AccessInfo(ring, domain);
    }

    /**
     * Special Getter
     * @return Type of bank (has some impact on the validity/meaning of other fields
     *          BD.TYPE:Word 0 Bits 8-11
     */
    public BankType getBankType(
    ) {
        int code = ((int)(get(0) >> 24)) & 017;
        return BankType.get(code);
    }

    /**
     * Special Getter
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
    public AbsoluteAddress getBaseAddress(
    ) {
        return new AbsoluteAddress((short) (get(3) >> 32),
                                   (int) get(2) & 0777777,
                                   (int) get(3) & 0xFFFF);
    }

    /**
     * Special Getter
     * @return position of this bank relative to the first bank describing a large or very large bank
     *          BD.DISP:Word 4 Bits 3-17
     */
    public int getDisplacement(
    ) {
        return (int)(get(4) >> 18) & 077777;
    }

    /**
     * Special Getter
     * @return Execute, Read, and Write permissions for ring/domain at a lower level
     *          BD.GAP:Word 0 Bits 0-2
     */
    public AccessPermissions getGeneraAccessPermissions(
    ) {
        return new AccessPermissions((int) (get(0) >> 33));
    }

    /**
     * Special Getter
     * @return G-flag
     *          Addressing_Exception interrupt results if this BD is accessed by normal bank-manipulation handling
     *          BD.G:Word 0 Bit 13
     */
    public boolean getGeneralFault(
    ) {
        return (get(0) & 020_000000L) != 0;
    }

    /**
     * Special Getter
     * @return large bank flag:
     *          If false, this is a single bank no greater than 0_777777.
     *              BD.Lower_Limit has 512-word granularity and BD.Upper_Limit has 1-word granularity.
     *              This is required for BD.Type other than ExtendedMode.
     *          IF true, this is either a portion of a large bank no greater than 077_777777
     *              or of a very large bank no greater than 077777_777777.
     *              BD.Lower_Limit has 32768-word granularity, and BD.Upper_Limit has 64-word granularity.
     *          BD.S:Word 0 Bit 15
     */
    public boolean getLargeBank(
    ) {
        return (get(0) & 04_000000L) != 0;
    }

    /**
     * Special Getter
     * @return lower limit, subject to granularity specified in the large-bank field - for non-indirect banks
     *          Word 1 Bits 0-8
     */
    public int getLowerLimit(
    ) {
        return (int)(get(1) >> 27);
    }

    /**
     * Special Getter
     * @return lower limit with granularity normalized out (making this 1-word granularity)
     */
    public int getLowerLimitNormalized(
    ) {
        return getLargeBank() ? (getLowerLimit() << 15) : (getLowerLimit() << 9);
    }

    /**
     * Special Getter
     * @return Execute, Read, and Write permissions for ring/domain at a lower level
     *          BD.SAP:Word 0 Bits 3-5
     */
    public AccessPermissions getSpecialAccessPermissions(
    ) {
        return new AccessPermissions((int)(get(0) >> 30) & 07);
    }

    /**
     * Special Getter
     * @return Target bank L,BDI (only for indirect banks)
     *          L (level) is the top 3 bits in a the wrapped 18-bit field
     *          BDI is the remaining (bottom) 15 bits
     */
    public int getTargetLBDI(
    ) {
        return (int)(get(0) >> 18);
    }

    /**
     * Special Getter
     * @return upper limit, subject to granularity specified in the large-bank field - for non-indirect banks
     *          Word 1 Bits 9-35 (bottom 3 quarter words)
     */
    public int getUpperLimit(
    ) {
        return (int)(get(1) & 0777_777777L);
    }

    /**
     * Special Getter
     * @return upper limit with granularity normalized out (making this 1-word granularity)
     */
    public int getUpperLimitNormalized(
    ) {
        return getLargeBank() ? (getUpperLimit() << 6) : getUpperLimit();
    }

    /**
     * Special Getter
     * @return U flag (upper limit suppression control)
     * //TODO re-word the following
     *          The purpose of U is to permit the BDs not within the last 16,777,216 words of a Very_Large_Bank
     *          to have a maximum Upper_Limit for full 24-bit indexing. For BD.U := 1, the logical upper limit of the
     *          Very_Large_Bank is more than the BD can describe were the BD subset to the last word. See 11.2,
     *          Very_Large_Bank BD Construction.
     *          When BD.U = 0, indicates transfer of the UL from the Bank_Descriptor to the Base_Register
     *          considering subsetting (see 4.6.6). When BD.U = 1, indicates that the B.UL := 0777777, regardless
     *          of any subsetting. Results are Architecturally_Undefined if BD.S ¹ 1 and U = 1.
     *          For BDs other than BD.Type = Extended_Mode, the BD.U Must_Be_Zero.
     *
     *          BD.U:Word 0 Bit 16
     */
    public boolean getUpperLimitSuppressionControl(
    ) {
        return (get(0) & 02_000000L) != 0;
    }

    public void setAccessLock(
        final AccessInfo accessInfo
    ) {
        long result = get(0) & 0_777777_000000L;
        result |= accessInfo.get() & 0777777;
        set(0, result);
    }

    /**
     * Special setter
     * @param value new bank type value
     */
    public void setBankType(
        final BankType value
    ) {
        long result = get(0) & 0_776077_777777L;
        result |= (long)(value._code) << 24;
        set(0, result);
    }

    /**
     * Special setter - see getBaseAddress() above
     * @param baseAddress new base address value
     */
    public void setBaseAddress(
        final AbsoluteAddress baseAddress
    ) {
        long word2 = get(2) & 0_777777_000000L;
        word2 |= baseAddress._segment;
        long word3 = (long)(baseAddress._upi & 017) << 32;
        word3 |= baseAddress._offset;
        set(2, word2);
        set(3, word3);
    }

    /**
     * Special setter
     * @param value new GAP
     */
    public void setGeneralAccessPermissions(
        final AccessPermissions value
    ) {
        long result = get(0) & 0_077777_777777L;
        result |= ((long)value.get() << 33);
        set(0, result);
    }

    /**
     * Special setter
     * @param value new value
     */
    public void setGeneralFault(
        final boolean value
    ) {
        long result = get(0) & 0_777757_777777L;
        if (value) {
            result |= 020_000000;
        }
        set(0, result);
    }

    /**
     * Special setter
     * @param value new value
     */
    public void setLargeBank(
        final boolean value
    ) {
        long result = get(0) & 0_777773_777777L;
        if (value) {
            result |= 04_000000;
        }
        set(0, result);
    }

    /**
     * Special setter
     * @param value new value
     */
    public void setLowerLimit(
        final int value
    ) {
        long result = get(1) & 0_000777_777777L;
        result |= (long)(value & 0777) << 27;
        set(1, result);
    }

    /**
     * Special setter
     * @param value new SAP
     */
    public void setSpecialAccessPermissions(
        final AccessPermissions value
    ) {
        long result = get(0) & 0_707777_777777L;
        result |= ((long)value.get() << 30);
        set(0, result);
    }

    /**
     * Special setter
     * @param value new value
     */
    public void setUpperLimit(
        final int value
    ) {
        long result = get(1) & 0_777000_000000L;
        result |= value & 0777_777777L;
        set(1, result);
    }

    /**
     * Special setter
     * @param value new value
     */
    public void setUpperLimitSuppressionControl(
        final boolean value
    ) {
        long result = get(0) & 0_777775_777777L;
        if (value) {
            result |= 02_000000;
        }
        set(0, result);
    }
}
