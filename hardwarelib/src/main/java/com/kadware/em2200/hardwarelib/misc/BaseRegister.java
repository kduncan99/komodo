/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.AccessInfo;
import com.kadware.em2200.baselib.AccessPermissions;
import com.kadware.em2200.baselib.Word36Array;
import com.kadware.em2200.baselib.Word36ArraySlice;
import com.kadware.em2200.hardwarelib.InventoryManager;
import com.kadware.em2200.hardwarelib.MainStorageProcessor;
import com.kadware.em2200.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.em2200.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.em2200.hardwarelib.interrupts.AddressingExceptionInterrupt;

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
    public final long _lowerLimitNormalized;

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
    public final Word36Array _storage;

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
     * Creates a Word36ArraySlice to represent the bank as defined by the other attributes of this object
     * @return as described, null if void or limits indicate no storage
     * @throws AddressingExceptionInterrupt if something is wrong with the values
     */
    private Word36ArraySlice getStorage(
    ) throws AddressingExceptionInterrupt{
        if ((_voidFlag) || (_lowerLimitNormalized > _upperLimitNormalized)) {
            return null;
        }

        try {
            int bankSize = (int) (_upperLimitNormalized - _lowerLimitNormalized + 1);
            MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(_baseAddress._upi);
            Word36Array mspStorage = msp.getStorage(_baseAddress._segment);
            return new Word36ArraySlice(mspStorage, _baseAddress._offset, bankSize);
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
        checkLimits();
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
     * @param storage word36 array slice representing the bank
     */
    public BaseRegister(
        final AbsoluteAddress baseAddress,
        final boolean largeSizeFlag,
        final int lowerLimitNormalized,
        final int upperLimitNormalized,
        final AccessInfo accessLock,
        final AccessPermissions generalAccessPermissions,
        final AccessPermissions specialAccessPermissions,
        final Word36Array storage
    ) {
        _baseAddress = baseAddress;
        _largeSizeFlag = largeSizeFlag;
        _lowerLimitNormalized = lowerLimitNormalized;
        _upperLimitNormalized = upperLimitNormalized;
        _accessLock = accessLock;
        _generalAccessPermissions = generalAccessPermissions;
        _specialAccessPermissions = specialAccessPermissions;
        _storage = storage;
        _voidFlag = false;
        checkLimits();
    }

    /**
     * Constructor for building a BaseRegister from a bank descriptor with no subsetting
     * @param bankDescriptor
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
        _voidFlag = false;
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
        _voidFlag = (values[0] & 0_000200_000000L) != 0;
        _largeSizeFlag = (values[0] & 0_000004_000000L) != 0;
        _accessLock = new AccessInfo(values[0] & 0777777);
        _lowerLimitNormalized =(int)(((values[1] >> 27) & 0777) << (_largeSizeFlag ? 15 : 9));
        _upperLimitNormalized = (int) ((values[1] & 0777777) << (_largeSizeFlag ? 6 : 0)) | (_largeSizeFlag ? 077 : 0);
        _baseAddress = new AbsoluteAddress((short) (values[3] >> 32),
                                           (int) (values[2] & 0x1FFFFFF),
                                           (int) values[3]);
        _storage = getStorage();
        checkLimits();
    }

    /**
     * Getter
     * @return lower limit with granularity depending upon large size flag
     */
    public int getLowerLimit(
    ) {
        return (int) (_largeSizeFlag ? _lowerLimitNormalized >> 15 : _lowerLimitNormalized >> 9);
    }

    /**
     * Getter
     * @return upper limit with granularity depending upon the large size flag
     */
    public int getUpperLimit(
    ) {
        return (int) (_largeSizeFlag ? _upperLimitNormalized >> 6 : _upperLimitNormalized);
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
