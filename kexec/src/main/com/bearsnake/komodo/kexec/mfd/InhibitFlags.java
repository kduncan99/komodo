/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class InhibitFlags {

    private boolean _isGuarded;
    private boolean _isUnloadInhibited;
    private boolean _isPrivate;
    private boolean _isAssignedExclusively;
    private boolean _isWriteOnly;
    private boolean _isReadOnly;

    public int compose() {
        int value = _isGuarded ? 040 : 0;
        value |= _isUnloadInhibited ? 020 : 0;
        value |= _isPrivate ? 010 : 0;
        value |= _isAssignedExclusively ? 004 : 0;
        value |= _isWriteOnly ? 002 : 0;
        value |= _isReadOnly ? 001 : 0;
        return value;
    }

    public InhibitFlags extract(final long value) {
        _isGuarded = (value & 040) != 0;
        _isUnloadInhibited = (value & 020) != 0;
        _isPrivate = (value & 010) != 0;
        _isAssignedExclusively = (value & 004) != 0;
        _isWriteOnly = (value & 002) != 0;
        _isReadOnly = (value & 001) != 0;

        return this;
    }

    public static InhibitFlags extractFrom(final int value) {
        var inf = new InhibitFlags();
        inf.extract(value);
        return inf;
    }

    public boolean isGuarded() { return _isGuarded; }
    public boolean isUnloadInhibited() { return _isUnloadInhibited; }
    public boolean isPrivate() { return _isPrivate; }
    public boolean isAssignedExclusively() { return _isAssignedExclusively; }
    public boolean isWriteOnly() { return _isWriteOnly; }
    public boolean isReadOnly() { return _isReadOnly; }

    public InhibitFlags setIsGuarded(final boolean value) { _isGuarded = value; return this; }
    public InhibitFlags setIsUnloadInhibited(final boolean value) { _isUnloadInhibited = value; return this; }
    public InhibitFlags setIsPrivate(final boolean value) { _isPrivate = value; return this; }
    public InhibitFlags setIsAssignedExclusively(final boolean value) { _isAssignedExclusively = value; return this; }
    public InhibitFlags setIsWriteOnly(final boolean value) { _isWriteOnly = value; return this; }
    public InhibitFlags setIsReadOnly(final boolean value) { _isReadOnly = value; return this; }
}
