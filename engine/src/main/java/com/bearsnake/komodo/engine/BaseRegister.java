/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Describes a base register - there are 32 of these, each describing a based bank.
 */
public class BaseRegister {

    public final AccessInfo _accessLock;                        // ring and domain for this bank
    public final AbsoluteAddress _baseAddress;                  // physical location of the described bank
    public final AccessPermissions _generalAccessPermissions;   // ERW permissions for access from a key of lower privilege
    public final boolean _largeSizeFlag;                        // If true, area does not exceed 2^24 bytes - else 2^18 bytes

    public final int _lowerLimitNormalized;                     // Relative address, lower limit - 24 bits significant.
                                                                //   Corresponds to first word/value in the storage subset
                                                                //   one-word-granularity normalized form of the lower limit,
                                                                //   accounting for large size flag

    public final AccessPermissions _specialAccessPermissions;   //  ERW permissions for access from a key of higher or equal privilege

    public final int _upperLimitNormalized;                     //  Relative address, upper limit - 24 bits significant
                                                                //    one-word-granularity normalized form of the upper limit,
                                                                //    accounting for large size flag

    public final boolean _voidFlag;                             //  if true, this is a void bank (no storage)

    /**
     * Standard Constructor, used for Void bank
     */
    public BaseRegister() {
        _accessLock = new AccessInfo();
        _baseAddress = new AbsoluteAddress((short) 0, 0, 0);
        _generalAccessPermissions = new AccessPermissions();
        _largeSizeFlag = false;
        _lowerLimitNormalized = 0;
        _specialAccessPermissions = new AccessPermissions();
        _upperLimitNormalized = 0;
        _voidFlag = true;
    }

    /**
     * Initial value constructor for a non-void bank
     * @param baseAddress              indicates UPI and offset indicating where in an MSP the storage for this bank is located
     * @param largeSizeFlag            indicates a large size bank
     * @param lowerLimitNormalized     actual normalized lower limit
     * @param upperLimitNormalized     actual normalized upper limit
     * @param accessLock               ring and domain for this bank
     * @param generalAccessPermissions access permissions for lower ring/domain
     * @param specialAccessPermissions access permissions for equal or higher ring/domain
     */
    public BaseRegister(
        final AbsoluteAddress baseAddress,
        final boolean largeSizeFlag,
        final int lowerLimitNormalized,
        final int upperLimitNormalized,
        final AccessInfo accessLock,
        final AccessPermissions generalAccessPermissions,
        final AccessPermissions specialAccessPermissions
    ) {
        _baseAddress = baseAddress;
        _largeSizeFlag = largeSizeFlag;
        _lowerLimitNormalized = lowerLimitNormalized;
        _upperLimitNormalized = upperLimitNormalized;
        _accessLock = accessLock;
        _generalAccessPermissions = generalAccessPermissions;
        _specialAccessPermissions = specialAccessPermissions;
        _voidFlag = _lowerLimitNormalized > _upperLimitNormalized;
    }

    /**
     * Constructor for building a BaseRegister from a bank descriptor with no subsetting
     * @param bankDescriptor source bank descriptor
     */
    public BaseRegister(
        final BankDescriptor bankDescriptor
    ) {
        _accessLock = bankDescriptor.getAccessLock();
        _baseAddress = bankDescriptor.getBaseAddress();
        _generalAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getGeneraAccessPermissions().canRead(),
                                                          bankDescriptor.getGeneraAccessPermissions().canWrite());
        _largeSizeFlag = bankDescriptor.getLargeBank();
        _lowerLimitNormalized = bankDescriptor.getLowerLimitNormalized();
        _specialAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getSpecialAccessPermissions().canRead(),
                                                          bankDescriptor.getSpecialAccessPermissions().canWrite());
        _upperLimitNormalized = bankDescriptor.getUpperLimitNormalized();
        _voidFlag = _lowerLimitNormalized > _upperLimitNormalized;
    }

    /**
     * Constructor for building a BaseRegister from a bank descriptor with subsetting.
     * This occurs when the caller wishes to access a bank larger than the D-field allows, by accessing consecutive
     * sections of said bank by basing those segments on consecutive base registers.
     * In this case, we add the given offset to the base offset from the BD, and adjust the lower and upper
     * limits accordingly.  Subsequent accesses proceed as desired by virtue of the fact that we've set
     * the base address in the bank register, along with the limits, in this fashion.
     * @param bankDescriptor source bank descriptor
     * @param offset         offset parameter for subsetting
     */
    public BaseRegister(
        final BankDescriptor bankDescriptor,
        final int offset
    ) {
        _accessLock = bankDescriptor.getAccessLock();
        _baseAddress = bankDescriptor.getBaseAddress();
        _generalAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getGeneraAccessPermissions().canRead(),
                                                          bankDescriptor.getGeneraAccessPermissions().canWrite());
        _largeSizeFlag = bankDescriptor.getLargeBank();

        int bdLowerNorm = bankDescriptor.getLowerLimitNormalized();
        _lowerLimitNormalized = (bdLowerNorm > offset) ? bdLowerNorm - offset : 0;
        _specialAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getSpecialAccessPermissions().canRead(),
                                                          bankDescriptor.getSpecialAccessPermissions().canWrite());
        _upperLimitNormalized = bankDescriptor.getUpperLimitNormalized() - offset;
        _voidFlag = (_upperLimitNormalized < 0) || (_lowerLimitNormalized > _upperLimitNormalized);
    }

    /**
     * Constructor for load base register direct (i.e., loading from 4-word packet in memory)
     *
     * @param values 4-word packet format as such:
     *               Word 0:  bit 1:  GAP read
     *               bit 2:  GAP write
     *               bit 4:  SAP read
     *               bit 5:  SAP write
     *               bit 10: void flag
     *               bit 15: large size flag
     *               bits 18-19: Access lock ring
     *               bits 20-35: Access lock domain
     *               Word 1:  bits 0-8: Lower Limit
     *               bits 18-35: Upper limit
     *               Word 2:  bits 18-35: MSBits of absolute address
     *               Word 3:  bits 0-36:  LSBits of absolute address
     *               <p>
     *               Lower Limit - if large size, lower limit has 32,768 word granularity (15 bit shift)
     *               otherwise, it has 512 word granularity (9 bit shift)
     *               Upper limit - if large size, upper limit has 64 word granularity (6 bit shift)
     *               otherwise, it has 1 word granularity (no shift)
     *               Absolute address is defined by the MSP architecture
     */
    public BaseRegister(
        final long[] values
    ) {
        _generalAccessPermissions = new AccessPermissions(false,
                                                          (values[0] & 0_200000_000000L) != 0,
                                                          (values[0] & 0_100000_000000L) != 0);
        _specialAccessPermissions = new AccessPermissions(false,
                                                          (values[0] & 0_020000_000000L) != 0,
                                                          (values[0] & 0_010000_000000L) != 0);
        _largeSizeFlag = (values[0] & 0_000004_000000L) != 0;
        _accessLock = new AccessInfo(values[0] & 0777777);
        _lowerLimitNormalized = (int) (((values[1] >> 27) & 0777) << (_largeSizeFlag ? 15 : 9));
        _upperLimitNormalized = (int) ((values[1] & 0777777) << (_largeSizeFlag ? 6 : 0)) | (_largeSizeFlag ? 077 : 0);
        _baseAddress = new AbsoluteAddress(values, 2);
        _voidFlag = ((values[0] & 0_000200_000000L) != 0) || (_lowerLimitNormalized > _upperLimitNormalized);
    }

    /**
     * Checks a relative address against this bank's limits.
     *
     * @param relativeAddress relative address of interest
     * @param fetchFlag       true if this is related to an instruction fetch, else false
     * @throws ReferenceViolationInterrupt if the address is outside of limits
     */
    void checkAccessLimits(
        final long relativeAddress,
        final boolean fetchFlag
    ) throws ReferenceViolationInterrupt {
        if ((relativeAddress < _lowerLimitNormalized) || (relativeAddress > _upperLimitNormalized)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
        }
    }

    /**
     * Checks a particular key against this bank's limits.
     *
     * @param fetchFlag  true if this is related to an instruction fetch, else false
     * @param readFlag   true if this will result in a read
     * @param writeFlag  true if this will result in a write
     * @param accessInfo access info of the client
     * @throws ReferenceViolationInterrupt if the address is outside of limits
     */
    void checkAccessLimits(
        final boolean fetchFlag,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessInfo accessInfo
    ) throws ReferenceViolationInterrupt {
        //  Choose GAP or SAP based on caller's accessInfo, but only if necessary
        if (readFlag || writeFlag) {
            boolean useSAP = ((accessInfo.getRing() < _accessLock.getRing())
                || (accessInfo.getDomain() == _accessLock.getDomain()));
            AccessPermissions bankPermissions = useSAP ? _specialAccessPermissions : _generalAccessPermissions;

            if (readFlag && !bankPermissions.canRead()) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, fetchFlag);
            }

            if (writeFlag && !bankPermissions.canWrite()) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, fetchFlag);
            }
        }
    }

    /**
     * Checks a relative address and a particular key against this bank's limits.
     *
     * @param relativeAddress relative address of interest
     * @param fetchFlag       true if this is related to an instruction fetch, else false
     * @param readFlag        true if this will result in a read
     * @param writeFlag       true if this will result in a write
     * @param accessInfo      access info of the client
     * @throws ReferenceViolationInterrupt if the address is outside of limits
     */
    void checkAccessLimits(
        final long relativeAddress,
        final boolean fetchFlag,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessInfo accessInfo
    ) throws ReferenceViolationInterrupt {
        checkAccessLimits(relativeAddress, fetchFlag);
        checkAccessLimits(fetchFlag, readFlag, writeFlag, accessInfo);
    }

    /**
     * Checks a range of relative addresses and a particular key against this bank's limits.
     *
     * @param relativeAddress relative address of interest
     * @param count           number of consecutive words of the access
     * @param readFlag        true if this will result in a read
     * @param writeFlag       true if this will result in a write
     * @param accessInfo      access info of the client
     * @throws ReferenceViolationInterrupt if the address is outside of limits
     */
    void checkAccessLimits(
        final long relativeAddress,
        final int count,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessInfo accessInfo
    ) throws ReferenceViolationInterrupt {
        if ((relativeAddress < _lowerLimitNormalized) || (relativeAddress + count - 1 > _upperLimitNormalized)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        checkAccessLimits(false, readFlag, writeFlag, accessInfo);
    }

    @Override
    public boolean equals(
        final Object obj
    ) {
        if (obj instanceof BaseRegister) {
            BaseRegister brobj = (BaseRegister) obj;
            if (brobj._voidFlag && _voidFlag) {
                return true;
            }

            return (brobj._accessLock.equals(_accessLock)
                && (brobj._baseAddress.equals(_baseAddress))
                //  we must not compare ._enter flags, as they are undefined in the base register
                //  so we have to compare the individual read and write flags instead
                && (brobj._generalAccessPermissions.canRead() == _generalAccessPermissions.canRead())
                && (brobj._generalAccessPermissions.canWrite() == _generalAccessPermissions.canWrite())
                && (brobj._largeSizeFlag == _largeSizeFlag)
                && (brobj._lowerLimitNormalized == _lowerLimitNormalized)
                //  as above
                && (brobj._specialAccessPermissions.canRead() == _specialAccessPermissions.canRead())
                && (brobj._specialAccessPermissions.canWrite() == _specialAccessPermissions.canWrite())
                && (brobj._upperLimitNormalized == _upperLimitNormalized));
        }

        return false;
    }

    /**
     * Retrieves the content of this base register in canonical/architecturally-correct format.
     * Format is as such:
     * Word 0:  bit 1:  GAP read
     * bit 2:  GAP write
     * bit 4:  SAP read
     * bit 5:  SAP write
     * bit 10: void flag
     * bit 15: large size flag
     * bits 18-19: Access lock ring
     * bits 20-35: Access lock domain
     * Word 1:  bits 0-8: Lower Limit
     * bits 18-35: Upper limit
     * Word 2:  bits 18-35: MSBits of absolute address
     * Word 3:  bits 0-36:  LSBits of absolute address
     * <p>
     * Lower Limit - if large size, lower limit has 32,768 word granularity (15 bit shift)
     * otherwise, it has 512 word granularity (9 bit shift)
     * Upper limit - if large size, upper limit has 64 word granularity (6 bit shift)
     * otherwise, it has 1 word granularity (no shift)
     * Absolute address is defined by the MSP architecture
     *
     * @return 4 word array of values
     */
    long[] getBaseRegisterWords(
    ) {
        long[] result = new long[4];
        for (int rx = 0; rx < 4; ++rx) {
            result[rx] = 0;
        }

        if (_generalAccessPermissions.canRead()) {
            result[0] |= 0_200000_000000L;
        }
        if (_generalAccessPermissions.canWrite()) {
            result[0] |= 0_100000_000000L;
        }
        if (_specialAccessPermissions.canRead()) {
            result[0] |= 0_020000_000000L;
        }
        if (_specialAccessPermissions.canWrite()) {
            result[0] |= 0_010000_000000L;
        }
        if (_voidFlag) {
            result[0] |= 0_000200_000000L;
        }
        if (_largeSizeFlag) {
            result[0] |= 0_000004_000000L;
        }
        result[0] |= (_accessLock.getRing()) << 16;
        result[0] |= _accessLock.getDomain();

        result[1] = ((long) _lowerLimitNormalized >> (_largeSizeFlag ? 15 : 9)) << 27;
        result[1] |= (long) _upperLimitNormalized >> (_largeSizeFlag ? 6 : 0);

        result[2] = _baseAddress._segment;
        result[3] = ((long) (_baseAddress._upiIndex) << 32) | _baseAddress._offset;

        return result;
    }

    /**
     * @return lower limit with granularity depending upon large size flag
     */
    public int getLowerLimit(
    ) {
        return (_largeSizeFlag ? _lowerLimitNormalized >> 15 : _lowerLimitNormalized >> 9);
    }

    /**
     * @return upper limit with granularity depending upon large size flag
     */
    public int getUpperLimit() {
        return (_largeSizeFlag ? _upperLimitNormalized >> 6 : _upperLimitNormalized);
    }

    @Override
    public int hashCode() {
        return _voidFlag ? 0 : _baseAddress.hashCode();
    }

    @Override
    public String toString() {
        long[] values = getBaseRegisterWords();
        return String.format("%012o %012o %012o %012o", values[0], values[1], values[2], values[3]);
    }
}
