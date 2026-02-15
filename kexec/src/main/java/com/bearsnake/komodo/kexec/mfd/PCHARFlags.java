/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import com.bearsnake.komodo.kexec.Granularity;

public class PCHARFlags {

    private Granularity _granularity;
    private boolean _isWordAddressable;

    public int compose() {
        int value = _granularity == Granularity.Position ? 040 : 0;
        value |= _isWordAddressable ? 010 : 0;
        return value;
    }

    public PCHARFlags extract(final long value) {
        _granularity = (value & 040) == 0 ? Granularity.Track : Granularity.Position;
        _isWordAddressable = (value & 010) != 0;

        return this;
    }

    public static PCHARFlags extractFrom(final int value) {
        var inf = new PCHARFlags();
        inf.extract(value);
        return inf;
    }

    public Granularity getGranularity() { return _granularity; }
    public boolean isWordAddressable() { return _isWordAddressable; }
    public PCHARFlags setGranularity(final Granularity value) { _granularity = value; return this; }
    public PCHARFlags setIsWordAddressable(final boolean value) { _isWordAddressable = value; return this; }
}
