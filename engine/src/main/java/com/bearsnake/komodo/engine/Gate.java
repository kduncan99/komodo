/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine;

public class Gate {

    // static methods ----------------------------------------------------------

    public static AccessPermissions getGeneralAccessPermissions(
        final long[] storage,
        final int offset
    ) {
        return new AccessPermissions((int)(storage[offset] >> 33) & 07);
    }

    public static AccessPermissions getSpecialAccessPermissions(
        final long[] storage,
        final int offset
    ) {
        return new AccessPermissions((int)(storage[offset] >> 30) & 07);
    }

    public static boolean isLibrary(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000040_000000L) != 0;
    }

    public static boolean isGotoInhibited(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000020_000000L) != 0;
    }

    public static boolean isDesignatorBitInhibited(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000010_000000L) != 0;
    }

    public static boolean isAccessKeyInhibited(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000004_000000L) != 0;
    }

    public static boolean isLatentParameter0Inhibited(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000002_000000L) != 0;
    }

    public static boolean isLatentParameter1Inhibited(
        final long[] storage,
        final int offset
    ) {
        return (storage[offset] & 0_000001_000000L) != 0;
    }

    public static AccessLock getAccessLock(
        final long[] storage,
        final int offset
    ) {
        return new AccessLock((int)(storage[offset] & 0_777777));
    }

    public static short getBankLevel(
        final long[] storage,
        final int offset
    ) {
        return (short)(storage[offset + 1] >> 33);
    }

    public static int getBankDescriptorIndex(
        final long[] storage,
        final int offset
    ) {
        return (int)(storage[offset + 1] >> 18) & 0_077777;
    }

    public static int getOffset(
        final long[] storage,
        final int offset
    ) {
        return (int)(storage[offset + 1] & 0_777777);
    }

    public static short getBasicModeBaseRegister(
        final long[] storage,
        final int offset
    ) {
        return (short)(((storage[offset + 2] >> 24) & 03) + 12);
    }

    public static int getDesignatorRegisterBits12To17(
        final long[] storage,
        final int offset
    ) {
        return (int)(storage[offset + 2] >> 18) & 0_077;
    }

    public static AccessKey getAccessKey(
        final long[] storage,
        final int offset
    ) {
        return new AccessKey((int)(storage[offset + 2] & 0_777777));
    }

    public static long getLatentParameter0(
        final long[] storage,
        final int offset
    ) {
        return storage[offset + 3] & 0_777777_777777L;
    }

    public static long getLatentParameter1(
        final long[] storage,
        final int offset
    ) {
        return storage[offset + 4] & 0_777777_777777L;
    }

    // instance methods --------------------------------------------------------

    private final AccessPermissions _generalAccessPermissions;
    private final AccessPermissions _specialAccessPermissions;
    private boolean _isLibrary;
    private boolean _isGotoInhibited;
    private boolean _isDesignatorBitInhibited;
    private boolean _isAccessKeyInhibited;
    private boolean _isLatentParameter0Inhibited;
    private boolean _isLatentParameter1Inhibited;
    private final AccessLock _accessLock;
    private short _bankLevel;
    private int _bankDescriptorIndex;
    private int _offset;
    private short _basicModeBaseRegister;
    private int _designatorRegisterBits12To17;
    private final AccessKey _accessKey;
    private long _latentParameter0;
    private long _latentParameter1;

    /**
     * Create a default Gate
     */
    public Gate() {
        _generalAccessPermissions = new AccessPermissions();
        _specialAccessPermissions = new AccessPermissions();
        _isLibrary = false;
        _isGotoInhibited = false;
        _isDesignatorBitInhibited = false;
        _isAccessKeyInhibited = false;
        _isLatentParameter0Inhibited = false;
        _isLatentParameter1Inhibited = false;
        _accessLock = new AccessLock();
        _bankLevel = 0;
        _bankDescriptorIndex = 0;
        _offset = 0;
        _basicModeBaseRegister = 0;
        _designatorRegisterBits12To17 = 0;
        _accessKey = new AccessKey();
        _latentParameter0 = 0;
        _latentParameter1 = 0;
    }

    /**
     * Create a Gate using the given parameters.
     */
    public Gate(
        final AccessPermissions generalAccessPermissions,
        final AccessPermissions specialAccessPermissions,
        final boolean isLibrary,
        final boolean isGotoInhibited,
        final boolean isDesignatorBitInhibited,
        final boolean isAccessKeyInhibited,
        final boolean isLatentParameter0Inhibited,
        final boolean isLatentParameter1Inhibited,
        final AccessLock accessLock,
        final short bankLevel,
        final int bankDescriptorIndex,
        final int offset,
        final short basicModeBaseRegister,
        final int designatorRegisterBits12To17,
        final AccessKey accessKey,
        final long latentParameter0,
        final long latentParameter1
    ) {
        _generalAccessPermissions = new AccessPermissions().setCanEnter(generalAccessPermissions.canEnter());
        _specialAccessPermissions = new AccessPermissions().setCanEnter(specialAccessPermissions.canEnter());
        _isLibrary = isLibrary;
        _isGotoInhibited = isGotoInhibited;
        _isDesignatorBitInhibited = isDesignatorBitInhibited;
        _isAccessKeyInhibited = isAccessKeyInhibited;
        _isLatentParameter0Inhibited = isLatentParameter0Inhibited;
        _isLatentParameter1Inhibited = isLatentParameter1Inhibited;
        _accessLock = accessLock;
        _bankLevel = bankLevel;
        _bankDescriptorIndex = bankDescriptorIndex;
        _offset = offset;
        _basicModeBaseRegister = basicModeBaseRegister;
        _designatorRegisterBits12To17 = designatorRegisterBits12To17;
        _accessKey = accessKey;
        _latentParameter0 = latentParameter0 & 0_777777_777777L;
        _latentParameter1 = latentParameter1 & 0_777777_777777L;
    }

    /**
     * Loads a BankDescriptor from a buffer containing the serialized form of a Gate.
     * @param buffer 8-word buffer containing the serialized form of a Gate.
     */
    public Gate(
        final long[] buffer,
        final int offset
    ) {
        _generalAccessPermissions = getGeneralAccessPermissions(buffer, 0).setCanRead(false).setCanWrite(false);
        _specialAccessPermissions = getSpecialAccessPermissions(buffer, 0).setCanRead(false).setCanWrite(false);
        _isLibrary = isLibrary(buffer, 0);
        _isGotoInhibited = isGotoInhibited(buffer, 0);
        _isDesignatorBitInhibited = isDesignatorBitInhibited(buffer, 0);
        _isAccessKeyInhibited = isAccessKeyInhibited(buffer, 0);
        _isLatentParameter0Inhibited = isLatentParameter0Inhibited(buffer, 0);
        _isLatentParameter1Inhibited = isLatentParameter1Inhibited(buffer, 0);
        _accessLock = getAccessLock(buffer, 0);
        _bankLevel = getBankLevel(buffer, 0);
        _bankDescriptorIndex = getBankDescriptorIndex(buffer, 0);
        _offset = getOffset(buffer, 0);
        _basicModeBaseRegister = getBasicModeBaseRegister(buffer, 0);
        _designatorRegisterBits12To17 = getDesignatorRegisterBits12To17(buffer, 0);
        _accessKey = getAccessKey(buffer, 0);
        _latentParameter0 = getLatentParameter0(buffer, 0);
        _latentParameter1 = getLatentParameter1(buffer, 0);
    }

    public AccessPermissions getGeneralAccessPermissions() { return _generalAccessPermissions; }
    public AccessPermissions getSpecialAccessPermissions() { return _specialAccessPermissions; }
    public boolean isLibrary() { return _isLibrary; }
    public boolean isGotoInhibited() { return _isGotoInhibited; }
    public boolean isDesignatorBitInhibited() { return _isDesignatorBitInhibited; }
    public boolean isAccessKeyInhibited() { return _isAccessKeyInhibited; }
    public boolean isLatentParameter0Inhibited() { return _isLatentParameter0Inhibited; }
    public boolean isLatentParameter1Inhibited() { return _isLatentParameter1Inhibited; }
    public AccessLock getAccessLock() { return _accessLock; }
    public short getBankLevel() { return _bankLevel; }
    public int getBankDescriptorIndex() { return _bankDescriptorIndex; }
    public int getOffset() { return _offset; }
    public short getBasicModeBaseRegister() { return _basicModeBaseRegister; }
    public int getDesignatorRegisterBits12To17() { return _designatorRegisterBits12To17; }
    public AccessKey getAccessKey() { return _accessKey; }
    public long getLatentParameter0() { return _latentParameter0; }
    public long getLatentParameter1() { return _latentParameter1; }

    public Gate setGeneralAccessPermissions(final AccessPermissions perms) {
        _generalAccessPermissions.set(perms).setCanRead(false).setCanWrite(false);
        return this;
    }
    public Gate setSpecialAccessPermissions(final AccessPermissions perms) {
        _specialAccessPermissions.set(perms).setCanRead(false).setCanWrite(false);
        return this;
    }
    public Gate setIsLibrary(final boolean flag) { _isLibrary = flag; return this; }
    public Gate setIsGotoInhibited(final boolean flag) { _isGotoInhibited = flag; return this; }
    public Gate setIsDesignatorBitInhibited(final boolean flag) { _isDesignatorBitInhibited = flag; return this; }
    public Gate setIsAccessKeyInhibited(final boolean flag) { _isAccessKeyInhibited = flag; return this; }
    public Gate setIsLatentParameter0Inhibited(final boolean flag) { _isLatentParameter0Inhibited = flag; return this; }
    public Gate setIsLatentParameter1Inhibited(final boolean flag) { _isLatentParameter1Inhibited = flag; return this; }
    public Gate setAccessLock(final AccessLock lock) { _accessLock.set(lock); return this; }
    public Gate setBankLevel(final short value) { _bankLevel = value; return this; }
    public Gate setBankDescriptorIndex(final int value) { _bankDescriptorIndex = value; return this; }
    public Gate setOffset(final int value) { _offset = value; return this; }
    public Gate setBasicModeBaseRegister(final short value) { _basicModeBaseRegister = value; return this; }
    public Gate setDesignatorRegisterBits12To17(final int value) { _designatorRegisterBits12To17 = value; return this; }
    public Gate setAccessKey(final AccessKey value) { _accessKey.set(value); return this; }
    public Gate setLatentParameter0(final long value) { _latentParameter0 = value & 0_777777_777777L; return this; }
    public Gate setLatentParameter1(final long value) { _latentParameter1 = value & 0_777777_777777L; return this; }

    /**
     * Serializes this BankDescriptor into the given buffer at the given offset.
     * @param buffer array to which we serialize this object
     * @param offset offset from the start of the array at which we should start writing this object's data
     */
    public void serialize(
        final long[] buffer,
        final int offset
    ) {
        long value0 = 0;
        value0 |= (long) _generalAccessPermissions.toComposite() << 33;
        value0 |= (long) _specialAccessPermissions.toComposite() << 30;
        value0 |= (_isLibrary ? 0_000040_000000L : 0);
        value0 |= (_isGotoInhibited ? 0_000020_000000L : 0);
        value0 |= (_isDesignatorBitInhibited ? 0_000010_000000L : 0);
        value0 |= (_isAccessKeyInhibited ? 0_000004_000000L : 0);
        value0 |= (_isLatentParameter0Inhibited ? 0_000002_000000L : 0);
        value0 |= (_isLatentParameter1Inhibited ? 0_000001_000000L : 0);
        value0 |= _accessLock.toComposite();

        long value1 = 0;
        value1 |= ((long)_bankLevel & 07L) << 33;
        value1 |= ((long)_bankDescriptorIndex & 0_077777) << 18;
        value1 |= ((long)_offset & 0_777777);

        long value2 = 0;
        value2 |= ((long)(_basicModeBaseRegister - 12) & 0_03) << 24;
        value2 |= ((long)_designatorRegisterBits12To17 & 0_077) << 18;
        value2 |= _accessKey.toComposite() & 0_777777;

        buffer[offset] = value0;
        buffer[offset + 1] = value1;
        buffer[offset + 2] = value2;
        buffer[offset + 3] = _latentParameter0 & 0_777777_777777L;
        buffer[offset + 4] = _latentParameter1 & 0_777777_777777L;
        buffer[offset + 5] = 0;
        buffer[offset + 6] = 0;
        buffer[offset + 7] = 0;
    }
}
