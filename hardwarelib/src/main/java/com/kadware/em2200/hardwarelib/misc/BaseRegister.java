/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.AccessInfo;
import com.kadware.em2200.baselib.AccessPermissions;
import com.kadware.em2200.baselib.ArraySlice;
import com.kadware.em2200.hardwarelib.InventoryManager;
import com.kadware.em2200.hardwarelib.MainStorageProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.em2200.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.em2200.hardwarelib.interrupts.AddressingExceptionInterrupt;
import com.kadware.em2200.hardwarelib.interrupts.ReferenceViolationInterrupt;

/**
 * Describes a base register - there are 32 of these, each describing a based bank.
 */
@SuppressWarnings("Duplicates")
public class BaseRegister {

    /**
     * Describes the ring/domain for the described bank
     */
    public final AccessInfo _accessLock;

    /**
     * Describes the physical location of the described bank.
     */
    public final AbsoluteAddress _baseAddress;

    /**
     * Execute, Read, and Write general permissions
     * For access from a ring of lower privilege (i.e., higher value)
     */
    public final AccessPermissions _generalAccessPermissions;

    /**
     * If true, the base register describes an area not exceeding 2^24 bytes.
     * Otherwise, it describes an area not exceeding 2^18 bytes.
     */
    public final boolean _largeSizeFlag;

    /**
     * Relative address, lower limit - 24 bits significant.
     * This value corresponds to the first word/value in the storage subset.
     * This is one-word-granularity normalized form the lowerLimit value according to the large size flag.
     * Original (un-normalized) value is limited to 9 bits.
     */
    public final int _lowerLimitNormalized;

    /**
     * Execute, Read, and Write special permissions
     * For access from a ring of higher or equal privilege (i.e., lower or equal value)
     */
    public final AccessPermissions _specialAccessPermissions;

    /**
     * An object which describes the entirety of the bank.
     * In actuality, it is a Word36AddrSubset which describes a subset of some MSP's storage.
     * This will be null if _voidFlag is set.
     */
    public final ArraySlice _storage;

    /**
     * Relative address, upper limit - 24 bits significant.
     * This is one-word-granularity normalized form the upperLimit value according to the large size flag.
     * Original (un-normalized) value is limited to 18 bits.
     */
    public final int _upperLimitNormalized;

    /**
     * If true, this register does not describe a storage area (it is a void bank)
     */
    public final boolean _voidFlag;

    /**
     * Debugging - make sure ll/ul are not out of bounds
     */
    //TODO remove later after it doesn't trip for a long time
    private void checkLimits() {
        if (_largeSizeFlag) {
            assert((_lowerLimitNormalized & 037700_077777) == 0);
            assert((_upperLimitNormalized & 037700_000077) == 077);
        } else {
            assert((_lowerLimitNormalized & 037777_000777) == 0);
            assert((_upperLimitNormalized & 037777_000000) == 0);
        }
    }

    /**
     * Creates a ArraySlice to represent the bank as defined by the other attributes of this object
     * @return as described, null if void or limits indicate no storage
     * @throws AddressingExceptionInterrupt if something is wrong with the values
     */
    private ArraySlice getStorage(
    ) throws AddressingExceptionInterrupt{
        if ((_voidFlag) || (_lowerLimitNormalized > _upperLimitNormalized)) {
            return null;
        }

        try {
            int bankSize = (_upperLimitNormalized - _lowerLimitNormalized + 1);
            MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(_baseAddress._upi);
            ArraySlice mspStorage = msp.getStorage(_baseAddress._segment);
            return new ArraySlice(mspStorage, _baseAddress._offset, bankSize);
        } catch (UPIProcessorTypeException | UPINotAssignedException ex) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                   0,
                                                   0);
        }
    }

    /**
     * Standard Constructor, used for Void bank
     */
    public BaseRegister(
    ) {
        _accessLock = new AccessInfo();
        _baseAddress = new AbsoluteAddress((short) 0, 0, 0);
        _generalAccessPermissions = new AccessPermissions(false, false, false);
        _largeSizeFlag = false;
        _lowerLimitNormalized = 0;
        _specialAccessPermissions = new AccessPermissions(false, false, false);
        _storage = null;
        _upperLimitNormalized = 0;
        _voidFlag = true;
    }

    /**
     * Initial value constructor for a non-void bank
     * @param baseAddress indicates UPI and offset indicating where in an MSP the storage for this bank is located
     * @param largeSizeFlag indicates a large size bank
     * @param lowerLimitNormalized actual normalized lower limit
     * @param upperLimitNormalized actual normalized upper limit
     * @param accessLock ring and domain for this bank
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
    ) throws AddressingExceptionInterrupt {
        _baseAddress = baseAddress;
        _largeSizeFlag = largeSizeFlag;
        _lowerLimitNormalized = lowerLimitNormalized;
        _upperLimitNormalized = upperLimitNormalized;
        _accessLock = accessLock;
        _generalAccessPermissions = generalAccessPermissions;
        _specialAccessPermissions = specialAccessPermissions;
        _storage = getStorage();
        _voidFlag = _lowerLimitNormalized >_upperLimitNormalized;
        checkLimits();
    }

    /**
     * Constructor for building a BaseRegister from a bank descriptor with no subsetting
     * @param bankDescriptor source bank descriptor
     */
    public BaseRegister(
        final BankDescriptor bankDescriptor
    ) throws AddressingExceptionInterrupt {
        _accessLock = bankDescriptor.getAccessLock();
        _baseAddress = bankDescriptor.getBaseAddress();
        _generalAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getGeneraAccessPermissions()._read,
                                                          bankDescriptor.getGeneraAccessPermissions()._write);
        _largeSizeFlag = bankDescriptor.getLargeBank();
        _lowerLimitNormalized = bankDescriptor.getLowerLimitNormalized();
        _specialAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getSpecialAccessPermissions()._read,
                                                          bankDescriptor.getSpecialAccessPermissions()._write);
        _upperLimitNormalized = bankDescriptor.getUpperLimitNormalized();
        _voidFlag = _lowerLimitNormalized > _upperLimitNormalized;
        _storage = getStorage();
        checkLimits();
    }

    /**
     * Constructor for building a BaseRegister from a bank descriptor with subsetting.
     * This occurs when the caller wishes to access a bank larger than the D-field allows, by accessing consecutive
     * sections of said bank by basing those segments on consecutive base registers.
     * In this case, we add the given offset to the base offset from the BD, and adjust the lower and upper
     * limits accordingly.  Subsequent accesses proceed as desired by virtue of the fact that we've set
     * the base address in the bank register, along with the limits, in this fashion.
     * @param bankDescriptor source bank descriptor
     * @param offset offset parameter for subsetting
     */
    public BaseRegister(
        final BankDescriptor bankDescriptor,
        final int offset
    ) throws AddressingExceptionInterrupt {
        _accessLock = bankDescriptor.getAccessLock();
        AbsoluteAddress bdAddress = bankDescriptor.getBaseAddress();
        _baseAddress = new AbsoluteAddress(bdAddress._upi, bdAddress._segment,bdAddress._offset + offset);
        _generalAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getGeneraAccessPermissions()._read,
                                                          bankDescriptor.getGeneraAccessPermissions()._write);
        _largeSizeFlag = bankDescriptor.getLargeBank();

        int bdLowerNorm = bankDescriptor.getLowerLimitNormalized();
        _lowerLimitNormalized = (bdLowerNorm > offset) ? bdLowerNorm - offset : 0;
        _specialAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getSpecialAccessPermissions()._read,
                                                          bankDescriptor.getSpecialAccessPermissions()._write);
        _upperLimitNormalized = bankDescriptor.getUpperLimitNormalized() - offset;
        _voidFlag = (_upperLimitNormalized < 0) || (_lowerLimitNormalized > _upperLimitNormalized);
        _storage = getStorage();
        checkLimits();
    }

    /**
     * Constructor for load base register direct (i.e., loading from 4-word packet in memory)
     * @param values 4-word packet format as such:
     * Word 0:  bit 1:  GAP read
     *          bit 2:  GAP write
     *          bit 4:  SAP read
     *          bit 5:  SAP write
     *          bit 10: void flag
     *          bit 15: large size flag
     *          bits 18-19: Access lock ring
     *          bits 20-35: Access lock domain
     * Word 1:  bits 0-8: Lower Limit
     *          bits 18-35: Upper limit
     * Word 2:  bits 18-35: MSBits of absolute address
     * Word 3:  bits 0-36:  LSBits of absolute address
     *
     * Lower Limit - if large size, lower limit has 32,768 word granularity (15 bit shift)
     *               otherwise, it has 512 word granularity (9 bit shift)
     * Upper limit - if large size, upper limit has 64 word granularity (6 bit shift)
     *               otherwise, it has 1 word granularity (no shift)
     * Absolute address is defined by the MSP architecture
     */
    public BaseRegister(
        final long[] values
    ) throws AddressingExceptionInterrupt {
        _generalAccessPermissions = new AccessPermissions(false,
                                                          (values[0] & 0_200000_000000L) != 0,
                                                          (values[0] & 0_100000_000000L) != 0);
        _specialAccessPermissions = new AccessPermissions(false,
                                                          (values[0] & 0_020000_000000L) != 0,
                                                          (values[0] & 0_010000_000000L) != 0);
        _largeSizeFlag = (values[0] & 0_000004_000000L) != 0;
        _accessLock = new AccessInfo(values[0] & 0777777);
        _lowerLimitNormalized =(int)(((values[1] >> 27) & 0777) << (_largeSizeFlag ? 15 : 9));
        _upperLimitNormalized = (int) ((values[1] & 0777777) << (_largeSizeFlag ? 6 : 0)) | (_largeSizeFlag ? 077 : 0);
        _baseAddress = new AbsoluteAddress((short) (values[3] >> 32),
                                           (int) (values[2] & 0x1FFFFFF),
                                           (int) values[3]);
        _voidFlag = ((values[0] & 0_000200_000000L) != 0) || (_lowerLimitNormalized > _upperLimitNormalized);
        _storage = _voidFlag ? null : getStorage();
        checkLimits();
    }

    /**
     * Getter
     * @return lower limit with granularity depending upon large size flag
     */
    public int getLowerLimit(
    ) {
        return (_largeSizeFlag ? _lowerLimitNormalized >> 15 : _lowerLimitNormalized >> 9);
    }

    /**
     * Checks a relative address against this bank's limits.
     * @param relativeAddress relative address of interest
     * @param fetchFlag true if this is related to an instruction fetch, else false
     * @param readFlag true if this will result in a read
     * @param writeFlag true if this will result in a write
     * @param accessInfo access info of the client
     * @throws ReferenceViolationInterrupt if the address is outside of limits
     */
    public void checkAccessLimits(
        final int relativeAddress,
        final boolean fetchFlag,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessInfo accessInfo
    ) throws ReferenceViolationInterrupt {
        if ((relativeAddress < _lowerLimitNormalized) || (relativeAddress > _upperLimitNormalized)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
        }

        //  Choose GAP or SAP based on caller's accessInfo, but only if necessary
        if (readFlag || writeFlag) {
            boolean useSAP = ((accessInfo._ring < _accessLock._ring) || (accessInfo._domain == _accessLock._domain));
            AccessPermissions bankPermissions = useSAP ? _specialAccessPermissions : _generalAccessPermissions;

            if (readFlag && !bankPermissions._read) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, fetchFlag);
            }

            if (writeFlag && !bankPermissions._write) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, fetchFlag);
            }
        }
    }

    /**
     * As above, but checks a range of storage limits
     * @param relativeAddress relative address of interest
     * @param count number of consecutive words of the access
     * @param readFlag true if this will result in a read
     * @param writeFlag true if this will result in a write
     * @param accessInfo access info of the client
     * @throws ReferenceViolationInterrupt if the address is outside of limits
     */
    public void checkAccessLimits(
        final int relativeAddress,
        final int count,
        final boolean readFlag,
        final boolean writeFlag,
        final AccessInfo accessInfo
    ) throws ReferenceViolationInterrupt {
        if ((relativeAddress < _lowerLimitNormalized) || (relativeAddress + count - 1 > _upperLimitNormalized)) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, false);
        }

        //  Choose GAP or SAP based on caller's accessInfo, but only if necessary
        if (readFlag || writeFlag) {
            boolean useSAP = ((accessInfo._ring < _accessLock._ring) || (accessInfo._domain == _accessLock._domain));
            AccessPermissions bankPermissions = useSAP ? _specialAccessPermissions : _generalAccessPermissions;

            if (readFlag && !bankPermissions._read) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.ReadAccessViolation, false);
            }

            if (writeFlag && !bankPermissions._write) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.WriteAccessViolation, false);
            }
        }
    }

    /**
     * Getter
     * @return upper limit with granularity depending upon the large size flag
     */
    public int getUpperLimit(
    ) {
        return (_largeSizeFlag ? _upperLimitNormalized >> 6 : _upperLimitNormalized);
    }

    /**
     * Retrieves the content of this base register in canonical/architecturally-correct format.
     * Format is as such:
     * Word 0:  bit 1:  GAP read
     *          bit 2:  GAP write
     *          bit 4:  SAP read
     *          bit 5:  SAP write
     *          bit 10: void flag
     *          bit 15: large size flag
     *          bits 18-19: Access lock ring
     *          bits 20-35: Access lock domain
     * Word 1:  bits 0-8: Lower Limit
     *          bits 18-35: Upper limit
     * Word 2:  bits 18-35: MSBits of absolute address
     * Word 3:  bits 0-36:  LSBits of absolute address
     *
     * Lower Limit - if large size, lower limit has 32,768 word granularity (15 bit shift)
     *               otherwise, it has 512 word granularity (9 bit shift)
     * Upper limit - if large size, upper limit has 64 word granularity (6 bit shift)
     *               otherwise, it has 1 word granularity (no shift)
     * Absolute address is defined by the MSP architecture
     * @return 4 word array of values
     */
    public long[] getBaseRegisterWords(
    ) {
        long[] result = new long[4];
        for (int rx = 0; rx < 4; ++rx) { result[rx] = 0; }

        if (_generalAccessPermissions._read) { result[0] |= 0_200000_000000L; }
        if (_generalAccessPermissions._write) { result[0] |= 0_100000_000000L; }
        if (_specialAccessPermissions._read) { result[0] |= 0_020000_000000L; }
        if (_specialAccessPermissions._write) { result[0] |= 0_010000_000000L; }
        if (_voidFlag) { result[0] |= 0_000200_000000L; }
        if (_largeSizeFlag) { result[0] |= 0_000004_000000L; }
        result[0] |= (_accessLock._ring) << 16;
        result[0] |= _accessLock._domain;

        result[1] = ((long)_lowerLimitNormalized >> (_largeSizeFlag ? 15 : 9)) << 27;
        result[1] |= (long)_upperLimitNormalized >> (_largeSizeFlag ? 6 : 0);

        result[2] = _baseAddress._segment;
        result[3] = ((long) (_baseAddress._upi) << 32) | _baseAddress._offset;

        return result;
    }
}
