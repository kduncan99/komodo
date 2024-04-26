/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

/**
 * Brief information regarding an existing file cycle within a file set info struct
 */
public class FileSetCycleInfo {

    private boolean _toBeCataloged;
    private boolean _toBeDropped;
    private int _absoluteCycle;
    private MFDRelativeAddress _mainItem0Address;

    public int getAbsoluteCycle() { return _absoluteCycle; }
    public MFDRelativeAddress getMainItem0Address() { return _mainItem0Address; }
    public final boolean isToBeCataloged() { return _toBeCataloged; }
    public final boolean isToBeDropped() { return _toBeDropped; }

    public FileSetCycleInfo setAbsoluteCycle(final int value) { _absoluteCycle = value; return this; }
    public FileSetCycleInfo setMainItem0Address(final MFDRelativeAddress value) { _mainItem0Address = value; return this; }
    public FileSetCycleInfo setToBeCataloged(final boolean value) { _toBeCataloged = value; return this; }
    public FileSetCycleInfo setToBeDropped(final boolean value) { _toBeDropped = value; return this; }
}
