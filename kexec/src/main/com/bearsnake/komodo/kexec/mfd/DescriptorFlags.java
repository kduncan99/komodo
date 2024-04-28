/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class DescriptorFlags {

    private boolean _isUnloaded;
    private boolean _isBackedUp;
    private boolean _saveOnCheckPoint;
    private boolean _toBeCataloged;
    private boolean _isTapeFile;
    private boolean _isRemovableDiskFile;
    private boolean _toBeWriteOnly;
    private boolean _toBeReadOnly;
    private boolean _toBeDropped;

    public int compose() {
        int value = _isUnloaded ? 0_4000 : 0;
        value |= _isBackedUp ? 0_2000 : 0;
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
        _isUnloaded = (value & 0_4000) != 0;
        _isBackedUp = (value & 0_2000) != 0;
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

    public boolean isUnloaded() { return _isUnloaded; }
    public boolean isBackedUp() { return _isBackedUp; }
    public boolean saveOnCheckpoint() { return _saveOnCheckPoint; }
    public boolean toBeCataloged() { return _toBeCataloged; }
    public boolean isTapeFile() { return _isTapeFile; }
    public boolean isRemovableDiskFile() { return _isRemovableDiskFile; }
    public boolean toBeWriteOnly() { return _toBeWriteOnly; }
    public boolean toBeReadOnly() { return _toBeReadOnly; }
    public boolean toBeDropped() { return _toBeDropped; }

    public DescriptorFlags setIsUnloaded(final boolean value) { _isUnloaded = value; return this; }
    public DescriptorFlags setIsBackedUp(final boolean value) { _isBackedUp = value; return this; }
    public DescriptorFlags setSaveOnCheckPoint(final boolean value) { _saveOnCheckPoint = value; return this; }
    public DescriptorFlags setToBeCataloged(final boolean value) { _toBeCataloged = value; return this; }
    public DescriptorFlags setIsTapeFile(final boolean value) { _isTapeFile = value; return this; }
    public DescriptorFlags setIsRemovableDiskFile(final boolean value) { _isRemovableDiskFile = value; return this; }
    public DescriptorFlags setToBeWriteOnly(final boolean value) { _toBeWriteOnly = value; return this; }
    public DescriptorFlags setToBeReadOnly(final boolean value) { _toBeReadOnly = value; return this; }
    public DescriptorFlags setToBeDropped(final boolean value) { _toBeDropped = value; return this; }
}
