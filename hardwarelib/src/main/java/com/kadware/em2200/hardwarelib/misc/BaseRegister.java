/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.AccessInfo;
import com.kadware.em2200.baselib.AccessPermissions;
import com.kadware.em2200.baselib.Word36Array;
import com.kadware.em2200.baselib.Word36ArraySlice;
import com.kadware.em2200.hardwarelib.MainStorageProcessor;

/**
 * Describes a base register - there are 32 of these, each describing a based bank.
 */
@SuppressWarnings("Duplicates")
public class BaseRegister {

    /**
     * Describes the ring/domain for the described bank
     */
    public final AccessInfo _accessLock;

    public final BankDescriptor.BankType _bankType;

    /**
     * Describes the physical location of the described bank.
     * It should be noted that banks can have a non-zero lower-limit address.  This address is the lowest acceptable value
     * for a relative address access into the bank, and corresponds to the first word in the bank.  Addressing algorithms
     * always add the base address value to the relative address (or displacement), which would cause an incorrect shift
     * for banks with non-zero lower limits.  Thus, all bank descriptors will adjust the base address downward by the
     * lower-limit value, so that the addresesing algorithm works properly.
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
     * This is one-word-granularity normalized form the lowerLimit value according to the large size flag
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
    public final Word36Array _storage;

    /**
     * Relative address, upper limit - 24 bits significant.
     * This is one-word-granularity normalized form the upperLimit value according to the large size flag
     */
    public final int _upperLimitNormalized;

    /**
     * If true, this register does not describe a storage area (it is a void bank)
     */
    public final boolean _voidFlag;

    /**
     * Standard Constructor, used for Void bank
     * @param bankType extended, basic, gate, indirect, queue
     */
    public BaseRegister(
        final BankDescriptor.BankType bankType
    ) {
        _accessLock = new AccessInfo();
        _bankType = bankType;
        _baseAddress = new AbsoluteAddress();
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
     * @param bankType extended, basic, gate, indirect, queue
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
        final BankDescriptor.BankType bankType,
        final AbsoluteAddress baseAddress,
        final boolean largeSizeFlag,
        final int lowerLimitNormalized,
        final int upperLimitNormalized,
        final AccessInfo accessLock,
        final AccessPermissions generalAccessPermissions,
        final AccessPermissions specialAccessPermissions,
        final Word36Array storage
    ) {
        _bankType = bankType;
        _baseAddress = baseAddress;
        _largeSizeFlag = largeSizeFlag;
        _lowerLimitNormalized = lowerLimitNormalized;
        _upperLimitNormalized = upperLimitNormalized;
        _accessLock = accessLock;
        _generalAccessPermissions = generalAccessPermissions;
        _specialAccessPermissions = specialAccessPermissions;
        _storage = storage;
        _voidFlag = false;
    }

    /**
     * Constructor for building a BaseRegister from a bank descriptor with no subsetting
     * @param bankDescriptor
     */
    public BaseRegister(
        final BankDescriptor bankDescriptor
    ) {
        _accessLock = bankDescriptor.getAccessLock();
        _bankType = bankDescriptor.getBankType();
        _baseAddress = bankDescriptor.getBaseAddress();
        _generalAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getGeneraAccessPermissions()._read,
                                                          bankDescriptor.getGeneraAccessPermissions()._write);
        _largeSizeFlag = bankDescriptor.getLargeBank();
        _lowerLimitNormalized = bankDescriptor.getLowerLimitNormalized();
        _specialAccessPermissions = new AccessPermissions(false,
                                                          bankDescriptor.getSpecialAccessPermissions()._read,
                                                          bankDescriptor.getSpecialAccessPermissions()._write);
        _storage = null;//TODO what to do here?
        _upperLimitNormalized = bankDescriptor.getUpperLimitNormalized();
        _voidFlag = false;
    }

    /**
     * Getter
     * @return lower limit with granularity depending upon large size flag
     */
    public int getLowerLimit(
    ) {
        return _largeSizeFlag ? _lowerLimitNormalized >> 15 : _lowerLimitNormalized >> 9;
    }

    /**
     * Getter
     * @return upper limit with granularity depending upon the large size flag
     */
    public int getUpperLimit(
    ) {
        return _largeSizeFlag ? _upperLimitNormalized >> 6 : _upperLimitNormalized;
    }
}
