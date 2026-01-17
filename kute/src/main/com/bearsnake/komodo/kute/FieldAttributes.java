/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

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

    public void setIntensity(Intensity intensity) { _intensity = intensity; }
    public void setBlinking(boolean blinking) { _blinking = blinking; }
    public void setReverseVideo(boolean reverseVideo) { _reverseVideo = reverseVideo; }
    public void setProtected(boolean isProtected) { _protected = isProtected; }
    public void setProtectedEmphasis(boolean isProtected) { _protectedEmphasis = isProtected; }
    public void setTabStop(boolean tabStop) { _tabStop = tabStop; }
    public void setAlphabeticOnly(boolean isAlphabeticOnly) { _alphabeticOnly = isAlphabeticOnly; }
    public void setNumericOnly(boolean isNumericOnly) { _numericOnly = isNumericOnly; }
    public void setRightJustified(boolean isRightJustified) { _rightJustified = isRightJustified; }
    public void setChanged(boolean isChanged) { _changed = isChanged; }
    public void setBackgroundColor(UTSColor backgroundColor) { _backgroundColor = backgroundColor; }
    public void setTextColor(UTSColor textColor) { _textColor = textColor; }

    public void setDisabled(){ _isEnabled = false; };
    public void setEnabled(){ _isEnabled = true; };
}
