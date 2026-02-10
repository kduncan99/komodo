/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.exceptions.BufferOverflowException;
import com.bearsnake.komodo.kutelib.exceptions.InvalidFCCSequenceException;
import com.bearsnake.komodo.kutelib.uts.UTSByteBuffer;
import com.bearsnake.komodo.kutelib.panes.Intensity;
import com.bearsnake.komodo.kutelib.panes.UTSColor;

public class FieldAttributes {

    private Intensity _intensity;
    private boolean _blinking;
    private boolean _reverseVideo;
    private boolean _protected;
    private boolean _protectedEmphasis;
    private boolean _tabStop;
    private boolean _alphabeticOnly;
    private boolean _numericOnly;
    private boolean _rightJustified;
    private boolean _changed;
    private UTSColor _backgroundColor;
    private UTSColor _textColor;

    public FieldAttributes() {
        _intensity = Intensity.NORMAL;
    }

    public FieldAttributes copy() {
        var attr = new FieldAttributes();
        attr._intensity = _intensity;
        attr._blinking = _blinking;
        attr._reverseVideo = _reverseVideo;
        attr._protected = _protected;
        attr._protectedEmphasis = _protectedEmphasis;
        attr._tabStop = _tabStop;
        attr._alphabeticOnly = _alphabeticOnly;
        attr._numericOnly = _numericOnly;
        attr._rightJustified = _rightJustified;
        attr._changed = _changed;
        attr._backgroundColor = _backgroundColor;
        attr._textColor = _textColor;
        return attr;
    }

    public Intensity getIntensity() { return _intensity; }
    public boolean isBlinking() { return _blinking; }
    public boolean isReverseVideo() { return _reverseVideo; }
    public boolean isProtected() { return _protected; }
    public boolean isProtectedEmphasis() { return _protectedEmphasis; }
    public boolean isTabStop() { return _tabStop; }
    public boolean isAlphabeticOnly() { return _alphabeticOnly; }
    public boolean isNumericOnly() { return _numericOnly; }
    public boolean isRightJustified() { return _rightJustified; }
    public boolean isChanged() { return _changed; }
    public UTSColor getBackgroundColor() { return _backgroundColor; }
    public UTSColor getTextColor() { return _textColor; }

    public boolean hasColor() { return (_backgroundColor != null) || (_textColor != null); }

    public FieldAttributes setIntensity(Intensity intensity) { _intensity = intensity; return this; }
    public FieldAttributes setBlinking(boolean blinking) { _blinking = blinking; return this; }
    public FieldAttributes setReverseVideo(boolean reverseVideo) { _reverseVideo = reverseVideo; return this; }
    public FieldAttributes setProtected(boolean isProtected) { _protected = isProtected; return this; }
    public FieldAttributes setProtectedEmphasis(boolean isProtected) { _protectedEmphasis = isProtected; return this; }
    public FieldAttributes setTabStop(boolean tabStop) { _tabStop = tabStop; return this; }
    public FieldAttributes setAlphabeticOnly(boolean isAlphabeticOnly) { _alphabeticOnly = isAlphabeticOnly; return this; }
    public FieldAttributes setNumericOnly(boolean isNumericOnly) { _numericOnly = isNumericOnly; return this; }
    public FieldAttributes setRightJustified(boolean isRightJustified) { _rightJustified = isRightJustified; return this; }
    public FieldAttributes setChanged(boolean isChanged) { _changed = isChanged; return this; }
    public FieldAttributes setBackgroundColor(UTSColor backgroundColor) { _backgroundColor = backgroundColor; return this; }
    public FieldAttributes setTextColor(UTSColor textColor) { _textColor = textColor; return this; }

    /**
     * Deserialize FCC field settings (M and N bytes, or O, M, and N bytes) from the source at the given offset.
     * @param source buffer containing an FCC sequence which we are deserializing
     */
    public static FieldAttributes deserialize(final UTSByteBuffer source)
        throws InvalidFCCSequenceException, BufferOverflowException {
        var fa = new FieldAttributes();
        var check = source.peekNext() & 0xF0;
        if (check == 0x20) {
            // we're doing color and expanded
            byte o = source.getNext();
            fa.deserializeMNExpanded( source);
            switch (o) {
                case 0x20 -> {
                    var c1 = source.getNext();
                    fa._textColor = UTSColor.fromByte((byte)(c1 & 0x07));
                    fa._backgroundColor = UTSColor.fromByte((byte)((c1 >> 3) & 0x07));
                }
                case 0x21 -> fa._textColor = UTSColor.fromByte(source.getNext());
                case 0x22 -> fa._backgroundColor = UTSColor.fromByte(source.getNext());
                case 0x23 -> {
                    fa._textColor = UTSColor.fromByte(source.getNext());
                    fa._backgroundColor = UTSColor.fromByte(source.getNext());
                }
                default -> throw new InvalidFCCSequenceException();
            }
        } else if (check == 0x30) {
            // we're doing basic only
            fa.deserializeMNBasic( source);
        } else if (check == 0x40) {
            // we're doing expanded only
            fa.deserializeMNExpanded( source);
        } else {
            throw new InvalidFCCSequenceException();
        }

        return fa;
    }

    public void deserializeMNBasic(final UTSByteBuffer source) throws BufferOverflowException {
        var m = source.getNext();
        switch (m & 0x03) {
            case 0x00 -> _intensity = Intensity.NORMAL;
            case 0x01 -> _intensity = Intensity.NONE;
            case 0x02 -> _intensity = Intensity.LOW;
            case 0x03 -> _blinking = true;
        }
        _changed = ((m & 0x04) == 0x04);
        _tabStop = ((m & 0x08) != 0x08);

        var n = source.getNext();
        _alphabeticOnly = ((n & 0x03) == 0x01);
        _numericOnly = ((n & 0x03) == 0x02);
        _protected = ((n & 0x03) == 0x03);
        _rightJustified = ((n & 0x04) == 0x04);
    }

    public void deserializeMNExpanded(final UTSByteBuffer source) throws BufferOverflowException {
        var m = source.getNext();
        switch (m & 0x03) {
            case 0x00 -> _intensity = Intensity.NORMAL;
            case 0x01, 0x03 -> _intensity = Intensity.NONE;
            case 0x02 -> _intensity = Intensity.LOW;
        }
        _changed = ((m & 0x04) == 0x04);
        _tabStop = ((m & 0x08) != 0x08);
        _protectedEmphasis = ((m & 0x20) == 0x20);

        var n = source.getNext();
        _alphabeticOnly = ((n & 0x03) == 0x01);
        _numericOnly = ((n & 0x03) == 0x02);
        _protected = ((n & 0x03) == 0x03);
        _rightJustified = ((n & 0x04) == 0x04);
        _blinking = ((n & 0x08) == 0x08);
        _reverseVideo = ((n & 0x10) == 0x10);
    }

    /**
     * Writes M and N bytes to the given buffer
     * @param buffer buffer to write to
     */
    public void serializeBasic(final UTSByteBuffer buffer) {
        byte m = 0x30;
        if (_blinking) {
            m |= 0x03;
        } else {
            switch (_intensity) {
                case NORMAL -> {
                }
                case LOW -> m |= 0x02;
                case NONE -> m |= 0x01;
            }
        }
        if (_changed) m |= 0x04;
        if (!_tabStop) m |= 0x08;

        byte n = 0x30;
        if (_alphabeticOnly) n |= 0x01;
        if (_numericOnly) n |= 0x02;
        if (_protected) n |= 0x03;
        if (_rightJustified) n |= 0x04;

        buffer.put(m).put(n);
    }

    /**
     * Writes M and N bytes to the given buffer if no color information exists,
     * else writes O, M, N, and C bytes.
     * @param buffer buffer to write to
     * @param withColor true if color information should be included, false otherwise
     */
    public void serializeExpanded(final UTSByteBuffer buffer,
                                  final boolean withColor) {
        byte o = 0x00;
        if (hasColor() && withColor) {
            if (_backgroundColor == null) {
                o = 0x21; // fg only
            } else if (_textColor == null) {
                o = 0x22; // bg only
            } else {
                o = 0x20; // fg/bg in one byte
            }
            buffer.put(o);
        }

        byte m = 0x40;
        if (_intensity == Intensity.NONE) m |= 0x01;
        if (_intensity == Intensity.LOW) m |= 0x02;
        if (_changed) m |= 0x04;
        if (!_tabStop) m |= 0x08;
        if (_protectedEmphasis) m |= 0x20;
        buffer.put(m);

        byte n = 0x40;
        if (_alphabeticOnly) n |= 0x01;
        if (_numericOnly) n |= 0x02;
        if (_protected) n |= 0x03;
        if (_rightJustified) n |= 0x04;
        if (_blinking) n |= 0x08;
        if (_reverseVideo) n |= 0x10;
        buffer.put(n);

        if (hasColor() && withColor) {
            byte c = 0x40;
            switch (o) {
                case 0x20 -> {
                    c |= _textColor.getByteValue();
                    c |= (byte)(_backgroundColor.getByteValue() << 3);
                }
                case 0x21 -> c |= _textColor.getByteValue();
                case 0x22 -> c |= _backgroundColor.getByteValue();
            }
            buffer.put(c);
        }
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(_intensity);
        if (_blinking) { sb.append(":").append("BLNK"); }
        if (_alphabeticOnly) { sb.append(":").append("ALPH"); }
        if (_numericOnly) { sb.append(":").append("NUM"); }
        if (_protectedEmphasis) { sb.append(":").append("PROT"); }
        if (_reverseVideo) { sb.append(":").append("REV"); }
        if (_rightJustified) { sb.append(":").append("RJST"); }
        if (_tabStop) { sb.append(":").append("TAB"); }
        if (_changed) { sb.append(":").append("CHG"); }
        if (_textColor != null) { sb.append(":FG-").append(_textColor); }
        if (_backgroundColor != null) { sb.append(":BG-").append(_backgroundColor); }

        return sb.toString();
    }
}
