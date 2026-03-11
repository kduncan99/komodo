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

    private ArraySlice _storage;
    private BankDescriptor _bankDescriptor;
    private long _subsetting; // offset from start of the underlying bank for this view

    public BaseRegister(
        final BankDescriptor bankDescriptor,
        final ArraySlice storage,
        final long subsetting
    ) {
        _bankDescriptor = bankDescriptor;
        _storage = storage;
        this._subsetting = subsetting;
    }

    public BankDescriptor getBankDescriptor() { return _bankDescriptor; }
    public ArraySlice getStorage() { return _storage; }
    public long getSubsetting() { return _subsetting; }
    public boolean isLargeBank() { return _bankDescriptor.isLargeBank(); }
    public boolean isVoid() { return _bankDescriptor == null; }

    public BaseRegister setBankDescriptor(
        final BankDescriptor bankDescriptor
    ) {
        _bankDescriptor = bankDescriptor;
        return this;
    }

    public BaseRegister setStorage(
        final ArraySlice storage
    ) {
        _storage = storage;
        return this;
    }

    public BaseRegister setSubsetting(
        final long subsetting
    ) {
        _subsetting = subsetting;
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
        if (_bankDescriptor == null) {
            throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
        } else {
            if ((relativeAddress < _bankDescriptor.getLowerLimitNormalized()) || (relativeAddress > _bankDescriptor.getUpperLimitNormalized())) {
                throw new ReferenceViolationInterrupt(ReferenceViolationInterrupt.ErrorType.StorageLimitsViolation, fetchFlag);
            }
        }
    }

    /**
     * Creates a void base register.
     */
    public static BaseRegister createVoid() {
        return new BaseRegister(null, null, 0);
    }

    /**
     * Loads this base register with the given bank descriptor and storage slice.
     * Subsetting is set to zero.
     * @param bankDescriptor bank descriptor
     * @param storage storage slice
     */
    public void fromBankDescriptor(
        final BankDescriptor bankDescriptor,
        final ArraySlice storage
    ) {
        _bankDescriptor = bankDescriptor;
        _storage = storage;
        _subsetting = 0;
    }

    /**
     * Loads this base register with the given bank descriptor, storage slice, and subsetting value.
     * We get into this mess when the caller wishes to access a bank larger than the D-field allows,
     * by accessing consecutive sections of said bank by basing those segments on consecutive base registers.
     * In this case, we add the given offset to the base offset from the BD, and adjust the lower and upper
     * limits accordingly.  Subsequent accesses proceed as desired by virtue of the fact that we've set
     * the base address in the bank register, along with the limits, in this fashion.
     * @param bankDescriptor bank descriptor
     * @param storage storage slice
     * @param subsetting offset from start of real bank
     */
    public void fromBankDescriptor(
        final BankDescriptor bankDescriptor,
        final ArraySlice storage,
        final long subsetting
    ) {
        _bankDescriptor = bankDescriptor;
        _storage = storage;
        _subsetting = subsetting;
    }

    public AccessPermissions getEffectivePermissions(
        final AccessKey key
    ) {
        var lock = _bankDescriptor.getAccessLock();
        var spec = _bankDescriptor.getSpecialAccessPermissions();
        var gen = _bankDescriptor.getGeneralAccessPermissions();
        return lock.getEffectivePermissions(key, gen, spec);
    }

    public BaseRegister makeVoid() {
        _bankDescriptor = null;
        _storage = null;
        _subsetting = 0;
        return this;
    }
}
