/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

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

    private boolean  _isEnabled = true;

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
    public boolean isProtected() { return _protected && _isEnabled; }
    public boolean isProtectedEmphasis() { return _protectedEmphasis && _isEnabled; }
    public boolean isTabStop() { return _tabStop; }
    public boolean isAlphabeticOnly() { return _alphabeticOnly && _isEnabled; }
    public boolean isNumericOnly() { return _numericOnly && _isEnabled; }
    public boolean isRightJustified() { return _rightJustified && _isEnabled; }
    public boolean isChanged() { return _changed; }
    public UTSColor getBackgroundColor() { return _backgroundColor; }
    public UTSColor getTextColor() { return _textColor; }

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

    public void setDisabled(){ _isEnabled = false; };
    public void setEnabled(){ _isEnabled = true; };
}
