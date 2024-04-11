/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import com.bearsnake.komodo.baselib.ArraySlice;

public class PackInfo implements MediaInfo {

    ArraySlice _label;
    boolean    _isFixed;
    boolean    _isPrepped;
    boolean    _isRemovable;
    String     _packName;
    int        _prepFactor;

    public PackInfo() {
        _label = null;
    }

    @Override
    public String getMediaName() { return _packName; }
}
