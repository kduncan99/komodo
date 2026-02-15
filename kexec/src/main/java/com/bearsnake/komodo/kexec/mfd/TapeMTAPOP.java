/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class TapeMTAPOP {

    private boolean _isHalfInchSerpentine;
    private boolean _isDLTCartridge;
    private boolean _isBufferedWriteOff;
    private boolean _isHalfInchCartridge;
    private boolean _isQuarterInchCartridge;
    private boolean _isCatalogedJOption;

    public boolean isHalfInchSerpentine() { return _isHalfInchSerpentine; }
    public boolean isDLTCartridge() { return _isDLTCartridge; }
    public boolean isBufferedWriteOff() { return _isBufferedWriteOff; }
    public boolean isHalfInchCartridge() { return _isHalfInchCartridge; }
    public boolean isQuarterInchCartridge() { return _isQuarterInchCartridge; }
    public boolean isCatalogedJOption() { return _isCatalogedJOption; }

    public TapeMTAPOP setIsHalfInchSerpentine(final boolean value) { _isHalfInchSerpentine = value; return this; }
    public TapeMTAPOP setIsDLTCartridge(final boolean value) { _isDLTCartridge = value; return this; }
    public TapeMTAPOP setIsBufferedWriteOff(final boolean value) { _isBufferedWriteOff = value; return this; }
    public TapeMTAPOP setIsHalfInchCartridge(final boolean value) { _isHalfInchCartridge = value; return this; }
    public TapeMTAPOP setIsQuarterInchCartridge(final boolean value) { _isQuarterInchCartridge = value; return this; }
    public TapeMTAPOP setIsCatalogedJOption(final boolean value) { _isCatalogedJOption = value; return this; }

    public int compose() {
        int result = _isHalfInchSerpentine ? 001 : 0;
        result |= _isDLTCartridge ? 002 : 0;
        result |= _isBufferedWriteOff ? 004 : 0;
        result |= _isHalfInchCartridge ? 010 : 0;
        result |= _isQuarterInchCartridge ? 020 : 0;
        result |= _isCatalogedJOption ? 040 : 0;
        return result;
    }

    public TapeMTAPOP extract(final long value) {
        _isHalfInchSerpentine = (value & 001) != 0;
        _isDLTCartridge = (value & 002) != 0;
        _isBufferedWriteOff = (value & 004) != 0;
        _isHalfInchCartridge = (value & 010) != 0;
        _isQuarterInchCartridge = (value & 020) != 0;
        _isCatalogedJOption = (value & 040) != 0;

        return this;
    }

    public static InhibitFlags extractFrom(final long value) {
        var inf = new InhibitFlags();
        inf.extract(value);
        return inf;
    }

}
