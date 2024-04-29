/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class TapeFormat {

    private boolean _dataConverterUsed;
    private boolean _isQuarterWord;
    private boolean _isSixBitPacked;
    private boolean _isEightBitPacked;
    private boolean _isEvenParity;
    private boolean _isNineTrack;

    public boolean getDataConverterUsed() { return _dataConverterUsed; }
    public boolean isQuarterWord() { return _isQuarterWord; }
    public boolean isSixBitPacked() { return _isSixBitPacked; }
    public boolean isEightBitPacked() { return _isEightBitPacked; }
    public boolean isEvenParity() { return _isEvenParity; }
    public boolean isNineTrack() { return _isNineTrack; }

    public TapeFormat setDataConverterUsed(final boolean value) { _dataConverterUsed = value; return this; }
    public TapeFormat setIsQuarterWord(final boolean value) { _isQuarterWord = value; return this; }
    public TapeFormat setIsSixBitPacked(final boolean value) { _isSixBitPacked = value; return this; }
    public TapeFormat setIsEightBitPacked(final boolean value) { _isEightBitPacked = value; return this; }
    public TapeFormat setIsEvenParity(final boolean value) { _isEvenParity = value; return this; }
    public TapeFormat setIsNineTrack(final boolean value) { _isNineTrack = value; return this; }

    public int compose() {
        int result = _dataConverterUsed ? 001 : 0;
        result |= _isQuarterWord ? 002 : 0;
        result |= _isSixBitPacked ? 004 : 0;
        result |= _isEightBitPacked ? 010 : 0;
        result |= _isEvenParity ? 020 : 0;
        result |= _isNineTrack ? 040 : 0;
        return result;
    }

    public TapeFormat extract(final long value) {
        _dataConverterUsed = (value & 001) != 0;
        _isQuarterWord = (value & 002) != 0;
        _isSixBitPacked = (value & 004) != 0;
        _isEightBitPacked = (value & 010) != 0;
        _isEvenParity = (value & 020) != 0;
        _isNineTrack = (value & 040) != 0;

        return this;
    }

    public static InhibitFlags extractFrom(final long value) {
        var inf = new InhibitFlags();
        inf.extract(value);
        return inf;
    }

}
