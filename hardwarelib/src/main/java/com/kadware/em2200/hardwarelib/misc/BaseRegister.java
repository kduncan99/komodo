/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib.misc;

import com.kadware.em2200.baselib.AccessInfo;
import com.kadware.em2200.baselib.AccessPermissions;
import com.kadware.em2200.baselib.Word36Array;

/**
 * Describes a base register - there are 32 of these, each describing a based bank.
 */
public class BaseRegister {

    //???? need unit tests
    /**
     * Describes the ring/domain for the described bank
     */
    private final AccessInfo _accessLock = new AccessInfo();

    /**
     * Describes the physical location of the described bank.
     * It should be noted that banks can have a non-zero lower-limit address.  This address is the lowest acceptable value
     * for a relative address access into the bank, and corresponds to the first word in the bank.  Addressing algorithms
     * always add the base address value to the relative address (or displacement), which would cause an incorrect shift
     * for banks with non-zero lower limits.  Thus, all bank descriptors will adjust the base address downward by the
     * lower-limit value, so that the addresesing algorithm works properly.
     */
    private final AbsoluteAddress _baseAddress = new AbsoluteAddress();

    /**
     * Execute, Read, and Write general permissions
     * For access from a ring of lower privilege (i.e., higher value)
     */
    private final AccessPermissions _generalAccessPermissions = new AccessPermissions();

    /**
     * If true, the base register describes an area not exceeding 2^24 bytes.
     * Otherwise, it describes an area not exceeding 2^18 bytes.
     */
    private boolean _largeSizeFlag;

    /**
     * Relative address, lower limit - 24 bits significant.
     * This value corresponds to the first word/value in the storage subset.
     * This is one-word-granularity normalized form the lowerLimit value according to the large size flag
     */
    private int _lowerLimitNormalized;

    /**
     * Execute, Read, and Write special permissions
     * For access from a ring of higher or equal privilege (i.e., lower or equal value)
     */
    private final AccessPermissions _specialAccessPermissions = new AccessPermissions();

    /**
     * An object which describes the entirety of the bank.
     * In actuality, it is a Word36AddrSubset which describes a subset of some MSP's storage.
     * This will be null if _voidFlag is set.
     */
    private Word36Array _storage = null;

    /**
     * Relative address, upper limit - 24 bits significant.
     * This is one-word-granularity normalized form the upperLimit value according to the large size flag
     */
    private int _upperLimitNormalized;

    /**
     * If true, this register does not describe a storage area (it is a void bank)
     */
    private boolean _voidFlag;

    /**
     * Standard Constructor, also used for Void bank
     */
    public BaseRegister(
    ) {
        _largeSizeFlag = false;
        _lowerLimitNormalized = 0;
        _upperLimitNormalized = 0;
        _voidFlag = true;
    }

    /**
     * Initial value constructor for a non-void bank
     * <p>
     * @param baseAddress
     * @param largeSizeFlag
     * @param lowerLimitNormalized actual normalized lower limit
     * @param upperLimitNormalized actual normalized upper limit
     * @param accessLock
     * @param generalAccessPermissions
     * @param specialAccessPermissions
     * @param storage
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
        _baseAddress.set(baseAddress);
        _largeSizeFlag = largeSizeFlag;
        _lowerLimitNormalized = lowerLimitNormalized;
        _upperLimitNormalized = upperLimitNormalized;
        _accessLock.set(accessLock);
        _generalAccessPermissions.set(generalAccessPermissions);
        _specialAccessPermissions.set(specialAccessPermissions);
        _storage = storage;
        _voidFlag = false;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public AccessInfo getAccessLock(
    ) {
        return _accessLock;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public AbsoluteAddress getBaseAddress(
    ) {
        return _baseAddress;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public AccessPermissions getGeneralAccessPermissions(
    ) {
        return _generalAccessPermissions;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getLargeSizeFlag(
    ) {
        return _largeSizeFlag;
    }

    /**
     * Getter
     * <p>
     * @return lower limit with granularity depending upon large size flag
     */
    public int getLowerLimit(
    ) {
        return _largeSizeFlag ? _lowerLimitNormalized >> 15 : _lowerLimitNormalized >> 9;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int getLowerLimitNormalized(
    ) {
        return _lowerLimitNormalized;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public AccessPermissions getSpecialAccessPermissions(
    ) {
        return _specialAccessPermissions;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Word36Array getStorage(
    ) {
        return _storage;
    }

    /**
     * Getter
     * <p>
     * @return upper limit with granularity depending upon the large size flag
     */
    public int getUpperLimit(
    ) {
        return _largeSizeFlag ? _upperLimitNormalized >> 6 : _upperLimitNormalized;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public int getUpperLimitNormalized(
    ) {
        return _upperLimitNormalized;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public boolean getVoidFlag(
    ) {
        return _voidFlag;
    }
}
