/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

import com.bearsnake.komodo.baselib.ArraySlice;

public class BankDescriptor {

    // static methods ----------------------------------------------------------

    public static AccessPermissions getGeneralAccessPermissions(
        final ArraySlice storage,
        final int offset
    ) {
        return new AccessPermissions((int)(storage.get(offset) >> 33) & 03);
    }

    public static AccessPermissions getSpecialAccessPermissions(
        final ArraySlice storage,
        final int offset
    ) {
        return new AccessPermissions((int)(storage.get(offset) >> 30) & 07);
    }

    public static BankType getBankType(
        final ArraySlice storage,
        final int offset
    ) {
        return BankType.get((int)(storage.get(offset) >> 24) & 0_017);
    }

    public static boolean isGeneralFault(
        final ArraySlice storage,
        final int offset
    ) {
        return (storage.get(offset) & 0_000020_000000L) != 0;
    }

    public static boolean isLargeBank(
        final ArraySlice storage,
        final int offset
    ) {
        return (storage.get(offset) & 0_000004_000000L) != 0;
    }

    public static AccessLock getAccessLock(
        final ArraySlice storage,
        final int offset
    ) {
        return new AccessLock((int)(storage.get(offset) & 0_777777));
    }

    public static long getIndirectLevelAndBDI(
        final ArraySlice storage,
        final int offset
    ) {
        return (storage.get(offset + 1) >> 18) & 0_777777L;
    }

    public static long getLowerLimit(
        final ArraySlice storage,
        final int offset
    ) {
        return (storage.get(offset + 1) >> 27) & 0777L;
    }

    public static long getUpperLimit(
        final ArraySlice storage,
        final int offset
    ) {
        return storage.get(offset + 1) & 0_777777L;
    }

    public static AbsoluteAddress getBaseAddress(
        final ArraySlice storage,
        final int offset
    ) {
        return new AbsoluteAddress(storage, 2);
    }

    public static long getInactiveQBDListNextPointer(
        final ArraySlice storage,
        final int offset
    ) {
        return storage.get(offset + 3);
    }

    public static long getDisplacement(
        final ArraySlice storage,
        final int offset
    ) {
        return (storage.get(offset + 4) >> 18) & 0_077777L;
    }

    public static boolean isInactive(
        final ArraySlice storage,
        final int offset
    ) {
        return (storage.get(offset + 4) & 0_400000_000000L) != 0;
    }

    // instance methods --------------------------------------------------------

    private final AccessPermissions _generalAccessPermissions;
    private final AccessPermissions _specialAccessPermissions;
    private BankType _bankType;
    private boolean _generalFault;
    private boolean _largeBank;
    private final AccessLock _accessLock;
    private long _indirectLevelAndBDI;
    private long _lowerLimit;
    private long _upperLimit;
    private boolean _inactive;
    private long _displacement;
    private final AbsoluteAddress _baseAddress;
    private long _inactiveQBDListNextPointer;

    public BankDescriptor() {
        _generalAccessPermissions = new AccessPermissions();
        _specialAccessPermissions = new AccessPermissions();
        _bankType = BankType.BasicMode;
        _generalFault = false;
        _largeBank = false;
        _accessLock = new AccessLock();
        _indirectLevelAndBDI = 0;
        _lowerLimit = 0;
        _upperLimit = 0;
        _inactive = true;
        _displacement = 0;
        _baseAddress = new AbsoluteAddress(0, 0);
        _inactiveQBDListNextPointer = 0;
    }

    public BankDescriptor(final boolean basicMode,
                          final AccessLock lock,
                          final AccessPermissions general,
                          final AccessPermissions special,
                          final AbsoluteAddress baseAddress,
                          final boolean largeBank,
                          final long actualLowerLimit,
                          final long actualUpperLimit,
                          final long displacement) {
        _bankType = basicMode ? BankType.BasicMode : BankType.ExtendedMode;
        _generalAccessPermissions = general;
        _specialAccessPermissions = special;
        _generalFault = false;
        _largeBank = largeBank;
        _accessLock = lock;
        _baseAddress = baseAddress;

        long ll = actualLowerLimit;
        long ul = actualUpperLimit;
        if (largeBank) {
            ll >>= 15;
            if ((actualLowerLimit & 077777) != 0) {
                ll += 1;
            }
            ul >>= 6;
            if ((actualUpperLimit & 077) != 0) {
                ul += 1;
            }
        } else {
            ll >>= 9;
            if ((actualLowerLimit & 0777) != 0) {
                ll += 1;
            }
        }

        _lowerLimit = ll;
        _upperLimit = ul;
        _inactive = false;
        _inactiveQBDListNextPointer = 0;
        _displacement = displacement;
    }

    public BankDescriptor(final long[] buffer) {
        _generalAccessPermissions = new AccessPermissions(
            (buffer[0] & 0_400000_000000L) != 0,
            (buffer[0] & 0_200000_000000L) != 0,
            (buffer[0] & 0_100000_000000L) != 0);
        _specialAccessPermissions = new AccessPermissions(
            (buffer[0] & 0_040000_000000L) != 0,
            (buffer[0] & 0_020000_000000L) != 0,
            (buffer[0] & 0_010000_000000L) != 0);
        _bankType = BankType.get((int)((buffer[0] >> 24) & 0x0F));
        _generalFault = (buffer[0] & 0_000020_000000L) != 0;
        _largeBank = (buffer[0] & 0_000004_000000L) != 0;
        _accessLock = new AccessLock(buffer[0] & 0x3FFFF);

        if (_bankType == BankType.Indirect) {
            _indirectLevelAndBDI = (buffer[1] >> 18) & 0_777777L;
        } else {
            _lowerLimit = (buffer[1] >> 27) & 0777L;
            _upperLimit = buffer[1] & 0_777777L;
        }

        _displacement = (buffer[4] >> 18) & 077777L;
        _inactive = (buffer[4] & 0_400000_000000L) != 0;

        _inactiveQBDListNextPointer = buffer[3];
        _baseAddress = new AbsoluteAddress(buffer, 2);
    }

    public AccessLock getAccessLock() { return _accessLock; }
    public BankType getBankType() { return _bankType; }
    public AbsoluteAddress getBaseAddress() { return _baseAddress; }
    public AccessPermissions getGeneralAccessPermissions() { return _generalAccessPermissions; }
    public long getIndirectLevelAndBDI() { return _indirectLevelAndBDI; }
    public long getLowerLimit() { return _lowerLimit; }
    public long getLowerLimitNormalized() { return _largeBank ? (_lowerLimit << 15) : (_lowerLimit << 9); }
    public AccessPermissions getSpecialAccessPermissions() { return _specialAccessPermissions; }
    public long getUpperLimit() { return _upperLimit; }
    public long getUpperLimitNormalized() { return _largeBank ? (_upperLimit << 6) : _upperLimit; }
    public boolean isGeneralFault() { return _generalFault; }
    public boolean isLargeBank() { return _largeBank; }
    public boolean isInactive() { return _inactive; }
    public long getDisplacement() { return _displacement; }
    public long getInactiveQBDListNextPointer() { return _inactiveQBDListNextPointer; }

    public BankDescriptor setAccessLock(final AccessLock lock) { _accessLock.set(lock); return this; }
    public BankDescriptor setBankType(final BankType bankType) { _bankType = bankType; return this; }
    public BankDescriptor setBaseAddress(final AbsoluteAddress baseAddress) { _baseAddress.set(baseAddress); return this; }
    public BankDescriptor setGeneralAccessPermissions(final AccessPermissions perms) { _generalAccessPermissions.set(perms); return this; }
    public BankDescriptor setSpecialAccessPermissions(final AccessPermissions perms) { _specialAccessPermissions.set(perms); return this; }
    public BankDescriptor setGeneralFault(final boolean flag) { _generalFault = flag; return this; }
    public BankDescriptor setLargeBank(final boolean flag) { _largeBank = flag; return this; }
    public BankDescriptor setIndirectLevelAndBDI(final long value) { _indirectLevelAndBDI = value; return this; }
    public BankDescriptor setLowerLimit(final long value) { _lowerLimit = value; return this; }
    public BankDescriptor setUpperLimit(final long value) { _upperLimit = value; return this; }
    public BankDescriptor setInactive(final boolean flag) { _inactive = flag; return this; }
    public BankDescriptor setDisplacement(final long value) { _displacement = value; return this; }
    public BankDescriptor setInactiveQBDListNextPointer(final long value) { _inactiveQBDListNextPointer = value; return this; }

    public void serialize(final long[] buffer) {
        long value0 = 0;
        value0 |= (long) _generalAccessPermissions.toComposite() << 33;
        value0 |= (long) _specialAccessPermissions.toComposite() << 30;
        value0 |= (long) _bankType._code << 24;
        if (_generalFault) {
            value0 |= 0_000020_000000L;
        }
        if (_largeBank) {
            value0 |= 0_000004_000000L;
        }
        value0 |= _accessLock.toComposite();

        long value1 = 0;
        if (_bankType == BankType.Indirect) {
            value1 |= _indirectLevelAndBDI << 18;
        } else {
            value1 |= _lowerLimit << 27;
            value1 |= _upperLimit;
        }

        long value2;
        long value3;
        if (_bankType == BankType.Queue && _inactive) {
            value2 = 0;
            value3 = _inactiveQBDListNextPointer;
        } else {
            long[] addrWords = new long[2];
            _baseAddress.populate(new ArraySlice(addrWords), 0);
            value2 = addrWords[0];
            value3 = addrWords[1];
        }

        long value4 = 0;
        if (_inactive) {
            value4 |= 0_400000_000000L;
        }
        value4 |= (_displacement & 077777L) << 18;

        buffer[0] = value0;
        buffer[1] = value1;
        buffer[2] = value2;
        buffer[3] = value3;
        buffer[4] = value4;
        buffer[5] = 0;
    }
}
