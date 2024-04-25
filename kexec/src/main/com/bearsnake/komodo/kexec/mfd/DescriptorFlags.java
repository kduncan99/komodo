/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class DescriptorFlags {

    private boolean _unloaded;
    private boolean _backedUp;
    private boolean _saveOnCheckPoint;
    private boolean _toBeCataloged;
    private boolean _isTapeFile;
    private boolean _isRemovableDiskFile;
    private boolean _toBeWriteOnly;
    private boolean _toBeReadOnly;
    private boolean _toBeDropped;

    public int compose() {
        int value = _unloaded ? 0_4000 : 0;
        value |= _backedUp ? 0_2000 : 0;
        value |= _saveOnCheckPoint ? 0_1000 : 0;
        value |= _toBeCataloged ? 0_0100 : 0;
        value |= _isTapeFile ? 0_0040 : 0;
        value |= _isRemovableDiskFile ? 0_0010 : 0;
        value |= _toBeWriteOnly ? 0_0004 : 0;
        value |= _toBeReadOnly ? 0_0002 : 0;
        value |= _toBeDropped ? 0_0001 : 0;
        return value;
    }

    public void extract(final int value) {
        _unloaded = (value & 0_4000) != 0;
        _backedUp = (value & 0_2000) != 0;
        _saveOnCheckPoint = (value & 0_1000) != 0;
        _toBeCataloged = (value & 0_0100) != 0;
        _isTapeFile = (value & 0_0040) != 0;
        _isRemovableDiskFile = (value & 0_0010) != 0;
        _toBeWriteOnly = (value & 0_0004) != 0;
        _toBeReadOnly = (value & 0_0002) != 0;
        _toBeDropped = (value & 0_0001) != 0;
    }

    public static DescriptorFlags extractFrom(final int value) {
        var inf = new DescriptorFlags();
        inf.extract(value);
        return inf;
    }
}
