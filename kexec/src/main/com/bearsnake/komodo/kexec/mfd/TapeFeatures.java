/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class TapeFeatures {

    // features
    private boolean _isDataCompressionOptional;
    private boolean _isDataCompressionOff;
    private boolean _isDataCompressionOn;
    private boolean _isBlockNumberingOptional;
    private boolean _isBlockNumberingOff;
    private boolean _isBlockNumberingOn;
    // features extension
    private boolean _isBufferedWriteTape;
    private boolean _is9940;
    private boolean _isDVD;
    private boolean _isBufferedWriteFile;
    private boolean _isBufferedWriteOn;
    private boolean _isLZ1DataCompression;
    private boolean _isExpandedBufferOptional;
    private boolean _isEDRCOptional;
    private boolean _isEDRCCompression;
    private boolean _isEightBitCompression;
    private boolean _isNineBitCompression;
    private boolean _isDataTranslation;
    // features extension_1
    private boolean _is9840C;
    private boolean _isLTO;
    private boolean _is9840D;
    private boolean _isT10000;

    public boolean isDataCompressionOptional() { return _isDataCompressionOptional; }
    public boolean isDataCompressionOff() { return _isDataCompressionOff; }
    public boolean isDataCompressionOn() { return _isDataCompressionOn; }
    public boolean isBlockNumberingOptional() { return _isBlockNumberingOptional; }
    public boolean isBlockNumberingOff() { return _isBlockNumberingOff; }
    public boolean isBlockNumberingOn() { return _isBlockNumberingOn; }

    public boolean isBufferedWriteTape() { return _isBufferedWriteTape; }
    public boolean is9940() { return _is9940; }
    public boolean isDVD() { return _isDVD; }
    public boolean isBufferedWriteFile() { return _isBufferedWriteFile; }
    public boolean isBufferedWriteOn() { return _isBufferedWriteOn; }
    public boolean isLZ1DataCompression() { return _isLZ1DataCompression; }
    public boolean isExpandedBufferOptional() { return _isExpandedBufferOptional; }
    public boolean isEDRCOptional() { return _isEDRCOptional; }
    public boolean isEDRCCompression() { return _isEDRCCompression; }
    public boolean isEightBitCompression() { return _isEightBitCompression; }
    public boolean isNineBitCompression() { return _isNineBitCompression; }
    public boolean isDataTranslation() { return _isDataTranslation; }

    public boolean is9840C() { return _is9840C; }
    public boolean isLTO() { return _isLTO; }
    public boolean is9840D() { return _is9840D; }
    public boolean isT10000() { return _isT10000; }

    public TapeFeatures setIsDataCompressionOptional(final boolean value) { _isDataCompressionOptional = value; return this; }
    public TapeFeatures setIsDataCompressionOff(final boolean value) { _isDataCompressionOff = value; return this; }
    public TapeFeatures setIsDataCompressionOn(final boolean value) { _isDataCompressionOn = value; return this; }
    public TapeFeatures setIsBlockNumberingOptional(final boolean value) { _isBlockNumberingOptional = value; return this; }
    public TapeFeatures setIsBlockNumberingOff(final boolean value) { _isBlockNumberingOff = value; return this; }
    public TapeFeatures setIsBlockNumberingOn(final boolean value) { _isBlockNumberingOn = value; return this; }

    public TapeFeatures setIsBufferedWriteTape(final boolean value) { _isBufferedWriteTape = value; return this; }
    public TapeFeatures setIs9940(final boolean value) { _is9940 = value; return this; }
    public TapeFeatures setIsDVD(final boolean value) { _isDVD = value; return this; }
    public TapeFeatures setIsBufferedWriteFile(final boolean value) { _isBufferedWriteFile = value; return this; }
    public TapeFeatures setIsBufferedWriteOn(final boolean value) { _isBufferedWriteOn = value; return this; }
    public TapeFeatures setIsLZ1DataCompression(final boolean value) { _isLZ1DataCompression = value; return this; }
    public TapeFeatures setIsExpandedBufferOptional(final boolean value) { _isExpandedBufferOptional = value; return this; }
    public TapeFeatures setIsEDRCCompression(final boolean value) { _isEDRCCompression = value; return this; }
    public TapeFeatures setIsEightBitCompression(final boolean value) { _isEightBitCompression = value; return this; }
    public TapeFeatures setIsNineBitCompression(final boolean value) { _isNineBitCompression = value; return this; }
    public TapeFeatures setIsDataTranslation(final boolean value) { _isDataTranslation = value; return this; }

    public TapeFeatures setIs9840C(final boolean value) { _is9840C = value; return this; }
    public TapeFeatures setIsLTO(final boolean value) { _isLTO = value; return this; }
    public TapeFeatures setIs9840D(final boolean value) { _is9840D = value; return this; }
    public TapeFeatures setIsT10000(final boolean value) { _isT10000 = value; return this; }

    public int compose() {
        int result = _isDataCompressionOptional ? 001 : 0;
        result |= _isDataCompressionOff ? 002 : 0;
        result |= _isDataCompressionOn ? 004 : 0;
        result |= _isBlockNumberingOptional ? 010 : 0;
        result |= _isBlockNumberingOff ? 020 : 0;
        result |= _isBlockNumberingOn ? 040 : 0;
        return result;
    }

    public int composeExtension() {
        int result = _isBufferedWriteTape ? 01 : 0;
        result |= _is9940 ? 02 : 0;
        result |= _isDVD ? 04 : 0;
        result |= _isBufferedWriteFile ? 010 : 0;
        result |= _isBufferedWriteOn ? 020 : 0;
        result |= _isLZ1DataCompression ? 040 : 0;
        result |= _isExpandedBufferOptional ? 0100 : 0;
        result |= _isEDRCOptional ? 0200 : 0;
        result |= _isEDRCCompression ? 0400 : 0;
        result |= _isEightBitCompression ? 01000 : 0;
        result |= _isNineBitCompression ? 02000 : 0;
        result |= _isDataTranslation ? 04000 : 0;
        return result;
    }

    public int composeExtension1() {
        int result = _is9840C ? 040 : 0;
        result |= _isLTO ? 020 : 0;
        result |= _is9840D ? 010 : 0;
        result |= _isT10000 ? 004 : 0;
        return result;
    }

    public TapeFeatures extract(final long value) {
        _isDataCompressionOptional = (value & 001) != 0;
        _isDataCompressionOff = (value & 002) != 0;
        _isDataCompressionOn = (value & 004) != 0;
        _isBlockNumberingOptional = (value & 010) != 0;
        _isBlockNumberingOff = (value & 020) != 0;
        _isBlockNumberingOn = (value & 040) != 0;

        return this;
    }

    public TapeFeatures extractExtension(final long value) {
        _isBufferedWriteTape = (value & 01) != 0;
        _is9940 = (value & 02) != 0;
        _isDVD = (value & 04) != 0;
        _isBufferedWriteFile = (value & 010) != 0;
        _isBufferedWriteOn = (value & 020) != 0;
        _isLZ1DataCompression = (value & 040) != 0;
        _isExpandedBufferOptional = (value & 0100) != 0;
        _isEDRCOptional = (value & 0200) != 0;
        _isEDRCCompression = (value & 0400) != 0;
        _isEightBitCompression = (value & 01000) != 0;
        _isNineBitCompression = (value & 02000) != 0;
        _isDataCompressionOff = (value & 04000) != 0;
        return this;
    }

    public TapeFeatures extractExtension1(final long value) {
        _is9840C = (value & 040) != 0;
        _isLTO = (value & 020) != 0;
        _is9840D = (value & 010) != 0;
        _isT10000 = (value & 004) != 0;

        return this;
    }
}
