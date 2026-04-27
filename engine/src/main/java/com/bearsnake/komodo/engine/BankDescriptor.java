/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

public class BankDescriptor {

    // static methods ----------------------------------------------------------

    public static AccessPermissions getGeneralAccessPermissions(
        final long[] storage,
        final int offset
    ) {
        return new AccessPermissions((int)(storage[offset] >> 33) & 03);
    }

    public static AccessPermissions getSpecialAccessPermissions(
        final long[] storage,
        final int offset
    ) {
        return new AccessPermissions((int)(storage[offset] >> 30) & 07);
    }

    public static int getBankLength(
        final long[] storage,
        final int offset
    ) {
        return getUpperLimitNormalized(storage, offset) - getLowerLimitNormalized(storage, offset) + 1;
    }

    public static BankType getBankType(
        final long[] storage,
        final int offset
    ) {
        return BankType.get((int)(storage[offset] >> 24) & 0_017);
    }

    public static boolean isGeneralFault(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000020_000000L) != 0;
    }

    public static boolean isLargeBank(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000004_000000L) != 0;
    }

    public static AccessLock getAccessLock(
        final long[] storage,
        final int offset
    ) {
        return new AccessLock((int)(storage[offset] & 0_777777));
    }

    public static int getIndirectLevelAndBDI(
        final long[] storage,
        final int offset
    ) {
        return (int)(storage[offset + 1] >> 18) & 0_777777;
    }

    public static int getLowerLimit(
        final long[] storage,
        final int offset
    ) {
        return (int)(storage[offset + 1] >> 27) & 0777;
    }

    public static int getLowerLimitNormalized(
        final long[] storage,
        final int offset
    ) {
        var ll = getLowerLimit(storage, offset);
        var large = isLargeBank(storage, offset);
        return large ? ll << 15 : ll << 9;
    }

    public static int getUpperLimit(
        final long[] storage,
        final int offset
    ) {
        return (int)storage[offset + 1] & 0_777777;
    }

    public static int getUpperLimitNormalized(
        final long[] storage,
        final int offset
    ) {
        var ul = getUpperLimit(storage, offset);
        var large = isLargeBank(storage, offset);
        return large ? ul << 6 : ul;
    }

    public static long getBaseAddress(
        final long[] storage,
        final int offset
    ) {
        return AbsoluteAddress.encodeToLong((int)storage[offset + 2], (int)storage[offset + 3]);
    }

    public static long getInactiveQBDListNextPointer(
        final long[] storage,
        final int offset
    ) {
        return storage[offset + 3];
    }

    public static int getDisplacement(
        final long[] storage,
        final int offset
    ) {
        return (int)(storage[offset + 4] >> 18) & 0_077777;
    }

    public static boolean isInactive(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset + 4] & 0_400000_000000L) != 0;
    }

    // instance methods --------------------------------------------------------

    private final AccessPermissions _generalAccessPermissions;
    private final AccessPermissions _specialAccessPermissions;
    private BankType _bankType;
    private boolean _generalFault;
    private boolean _largeBank;
    private final AccessLock _accessLock;
    private int _indirectLevelAndBDI;
    private int _lowerLimit;
    private int _upperLimit;
    private boolean _inactive;
    private int _displacement;
    private long _baseAddress;
    private long _inactiveQBDListNextPointer;

    /**
     * Create a default (void) bank descriptor
     */
    public BankDescriptor() {
        _generalAccessPermissions = new AccessPermissions();
        _specialAccessPermissions = new AccessPermissions();
        _bankType = BankType.BasicMode;
        _generalFault = false;
        _largeBank = false;
        _accessLock = new AccessLock();
        _indirectLevelAndBDI = 0;
        _lowerLimit = 0_1000;
        _upperLimit = 0_0777;
        _inactive = true;
        _displacement = 0;
        _baseAddress = 0;
        _inactiveQBDListNextPointer = 0;
    }

    /**
     * Create a BankDescriptor using the given parameters.
     * This is for basic or extended mode bank descriptors - other bank descriptors are not supported with this method.
     * @param basicMode true if this is a basic mode bank descriptor, false if it is an extended mode bank descriptor
     * @param lock the access lock for this bank descriptor
     * @param general the general access permissions for this bank descriptor
     * @param special the special access permissions for this bank descriptor
     * @param baseAddress the base address for this bank descriptor
     * @param largeBank true if this is a large bank descriptor, false if it is a small bank descriptor
     * @param actualLowerLimit the actual lower limit for this bank descriptor
     * @param actualUpperLimit the actual upper limit for this bank descriptor
     * @param displacement the displacement for this bank descriptor
     */
    public BankDescriptor(final boolean basicMode,
                          final AccessLock lock,
                          final AccessPermissions general,
                          final AccessPermissions special,
                          final long baseAddress,
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

        _lowerLimit = (int)ll;
        _upperLimit = (int)ul;
        _inactive = false;
        _inactiveQBDListNextPointer = 0;
        _displacement = (int)displacement;
    }

    /**
     * Loads a BankDescriptor from a buffer containing the serialized form of a BD.
     * @param buffer 8-word buffer containing the serialized form of a BD.
     */
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
            _indirectLevelAndBDI = (int)(buffer[1] >> 18) & 0_777777;
        } else {
            _lowerLimit = (int)(buffer[1] >> 27) & 0777;
            _upperLimit = (int)buffer[1] & 0_777777;
        }

        _displacement = (int)(buffer[4] >> 18) & 077777;
        _inactive = (buffer[4] & 0_400000_000000L) != 0;

        _inactiveQBDListNextPointer = buffer[3];
        _baseAddress = AbsoluteAddress.encodeToLong((int)buffer[2], (int)buffer[3]);
    }

    public AccessLock getAccessLock() { return _accessLock; }
    public BankType getBankType() { return _bankType; }
    public long getBaseAddress() { return _baseAddress; }
    public AccessPermissions getGeneralAccessPermissions() { return _generalAccessPermissions; }
    public long getIndirectLevelAndBDI() { return _indirectLevelAndBDI; }
    public int getLowerLimit() { return _lowerLimit; }
    public long getLowerLimitNormalized() { return (long)_lowerLimit << (_largeBank ? 15 : 9); }
    public AccessPermissions getSpecialAccessPermissions() { return _specialAccessPermissions; }
    public int getUpperLimit() { return _upperLimit; }
    public long getUpperLimitNormalized() { return (long)_upperLimit << (_largeBank ? 6 : 0); }
    public boolean isGeneralFault() { return _generalFault; }
    public boolean isLargeBank() { return _largeBank; }
    public boolean isInactive() { return _inactive; }
    public long getDisplacement() { return _displacement; }
    public long getInactiveQBDListNextPointer() { return _inactiveQBDListNextPointer; }

    public BankDescriptor setAccessLock(final AccessLock lock) { _accessLock.set(lock); return this; }
    public BankDescriptor setBankType(final BankType bankType) { _bankType = bankType; return this; }
    public BankDescriptor setBaseAddress(final long baseAddress) { _baseAddress = baseAddress; return this; }
    public BankDescriptor setGeneralAccessPermissions(final AccessPermissions perms) { _generalAccessPermissions.set(perms); return this; }
    public BankDescriptor setSpecialAccessPermissions(final AccessPermissions perms) { _specialAccessPermissions.set(perms); return this; }
    public BankDescriptor setGeneralFault(final boolean flag) { _generalFault = flag; return this; }
    public BankDescriptor setLargeBank(final boolean flag) { _largeBank = flag; return this; }
    public BankDescriptor setIndirectLevelAndBDI(final int value) { _indirectLevelAndBDI = value; return this; }
    public BankDescriptor setLowerLimit(final int value) { _lowerLimit = value; return this; }
    public BankDescriptor setUpperLimit(final int value) { _upperLimit = value; return this; }
    public BankDescriptor setInactive(final boolean flag) { _inactive = flag; return this; }
    public BankDescriptor setDisplacement(final int value) { _displacement = value; return this; }
    public BankDescriptor setInactiveQBDListNextPointer(final long value) { _inactiveQBDListNextPointer = value; return this; }

    /**
     * Serializes this BankDescriptor into the given buffer at the given offset.
     * @param slice slice containing the contiguous array to which we serialize this object
     * @param offset offset from the start of the slice at which we should start writing this object's data
     */
    public void serialize(
        final long[] slice,
        final int offset
    ) {
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
            value1 |= (long)_indirectLevelAndBDI << 18;
        } else {
            value1 |= (long)_lowerLimit << 27;
            value1 |= _upperLimit;
        }

        long value2;
        long value3;
        if (_bankType == BankType.Queue && _inactive) {
            value2 = 0;
            value3 = _inactiveQBDListNextPointer;
        } else {
            value2 = AbsoluteAddress.extractSegmentFromLong(_baseAddress);
            value3 = AbsoluteAddress.extractOffsetFromLong(_baseAddress);
        }

        long value4 = 0;
        if (_inactive) {
            value4 |= 0_400000_000000L;
        }
        value4 |= (_displacement & 077777L) << 18;

        slice[offset] = value0;
        slice[offset + 1] = value1;
        slice[offset + 2] = value2;
        slice[offset + 3] = value3;
        slice[offset + 4] = value4;
        slice[offset + 5] = 0;
        slice[offset + 6] = 0;
        slice[offset + 7] = 0;
    }
}
