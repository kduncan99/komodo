/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.engine.interrupts.ReferenceViolationInterrupt;

/**
 * Describes a base register - there are 32 of these, each describing a based bank.
 */
public class BaseRegister {

    private AccessPermissions _generalAccessPermissions;
    private AccessPermissions _specialAccessPermissions;
    private boolean _isVoid;
    private boolean _isLargeBank;
    private AccessLock _accessLock;
    private int _lowerLimit;
    private int _upperLimit;
    private AbsoluteAddress _baseAddress;

    private ArraySlice _storage;

    /**
     * Creates a void base register
     */
    public BaseRegister() {
        _isVoid = true;
        _accessLock = new AccessLock();
        _generalAccessPermissions = AccessPermissions.NONE;
        _specialAccessPermissions = AccessPermissions.NONE;
    }

    public AccessPermissions getGeneralAccessPermissions() { return _generalAccessPermissions; }
    public AccessPermissions getSpecialAccessPermissions() { return _specialAccessPermissions; }
    public boolean isVoid() { return _isVoid; }
    public boolean isLargeBank() { return _isLargeBank; }
    public AccessLock getAccessLock() { return _accessLock; }
    public int getLowerLimit() { return _lowerLimit; }
    public int getLowerLimitNormalized() { return _isLargeBank ? (_lowerLimit << 15) : (_lowerLimit << 9); }
    public int getUpperLimit() { return _upperLimit; }
    public int getUpperLimitNormalized() { return _isLargeBank ? (_upperLimit << 6) : _upperLimit; }
    public AbsoluteAddress getBaseAddress() { return _baseAddress; }
    public ArraySlice getStorage() { return _storage; }

    public BaseRegister setGeneralAccessPermissions(
        final AccessPermissions permissions
    ) {
        _generalAccessPermissions = permissions;
        return this;
    }

    public BaseRegister setSpecialAccessPermissions(
        final AccessPermissions permissions
    ) {
        _specialAccessPermissions = permissions;
        return this;
    }

    public BaseRegister setIsVoid(
        final boolean flag
    ) {
        _isVoid = flag;
        return this;
    }

    public BaseRegister setIsLargeBank(
        final boolean flag
    ) {
        _isLargeBank = flag;
        return this;
    }

    public BaseRegister setAccessLock(
        final AccessLock lock
    ) {
        _accessLock = lock;
        return this;
    }

    public BaseRegister setLowerLimit(
        final int limit
    ) {
        _lowerLimit = limit;
        return this;
    }

    public BaseRegister setUpperLimit(
        final int limit
    ) {
        _upperLimit = limit;
        return this;
    }

    public BaseRegister setBaseAddress(
        final AbsoluteAddress addr
    ) {
        _baseAddress = addr;
        return this;
    }

    public BaseRegister setStorage(
        final ArraySlice storage
    ) {
        _storage = storage;
        return this;
    }

    /**
     * Mostly for convenience for unit tests
     */
    public BaseRegister setLimitsNormalized(
        final boolean isLargeBank,
        final int lowerLimitNormalized,
        final int upperLimitNormalized
    ) {
        int lowerShift = isLargeBank ? 15 : 9;
        int upperShift = isLargeBank ? 6 : 0;
        assert((lowerLimitNormalized & (isLargeBank ? 077777 : 0777)) == 0);
        assert(!isLargeBank || ((upperLimitNormalized & 077) == 0));

        _isVoid = false;
        _isLargeBank = isLargeBank;
        _lowerLimit = lowerLimitNormalized >> (isLargeBank ? 15 : 9);
        _upperLimit = upperLimitNormalized >> (isLargeBank ? 6 : 0);
        return this;
    }

    /**
     * Verifies that the given relative address is within the limits defined
     * by the lower and upper normalized limits.
     * @param relativeAddress relative address to be compared
     * @param fetchFlag true if this is part of a fetch operation
     */
    public void checkAccessLimits(
        final long relativeAddress,
        final boolean fetchFlag
    ) throws ReferenceViolationInterrupt {
        // TODO should we check void flag?
        if ((relativeAddress < getLowerLimitNormalized()) || (relativeAddress > getUpperLimitNormalized())) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
        }
    }

    /**
     * Creates a void base register.
     */
    public static BaseRegister createVoid() {
        return new BaseRegister().setIsVoid(true).setStorage(null);
    }

    public AccessPermissions getEffectivePermissions(
        final AccessKey key
    ) {
        return _accessLock.getEffectivePermissions(key, _generalAccessPermissions, _specialAccessPermissions);
    }

    public BaseRegister makeVoid() {
        _isVoid = true;
        _storage = null;
        return this;
    }
}
