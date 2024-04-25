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

    public void extract(final int value) {
        _isGuarded = (value & 040) != 0;
        _isUnloadInhibited = (value & 020) != 0;
        _isPrivate = (value & 010) != 0;
        _isAssignedExclusively = (value & 004) != 0;
        _isWriteOnly = (value & 002) != 0;
        _isReadOnly = (value & 001) != 0;
    }

    public static InhibitFlags extractFrom(final int value) {
        var inf = new InhibitFlags();
        inf.extract(value);
        return inf;
    }
}
