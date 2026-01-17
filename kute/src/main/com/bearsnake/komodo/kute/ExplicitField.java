/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

public class ExplicitField extends Field {

    private boolean _alphabeticOnly;
    private UTSColor _backgroundColor;
    private boolean _blinking;
    private boolean _changed;
    private Intensity _intensity;
    private boolean _numericOnly;
    private boolean _protected;
    private boolean _protectedEmphasis;
    private boolean _reverseVideo;
    private boolean _rightJustified;
    private boolean _tabStop;
    private UTSColor _textColor;

    private boolean  _isEnabled = true;

    public ExplicitField(final Coordinates coordinates) {
        super(coordinates);
    }

    @Override
    public Field copy(final Coordinates coordinates) {
        var f = new ExplicitField(coordinates);
        f._alphabeticOnly = _alphabeticOnly;
        f._backgroundColor = _backgroundColor;
        f._blinking = _blinking;
        f._changed = false;
        f._intensity = _intensity;
        f._numericOnly = _numericOnly;
        f._protected = _protected;
        f._protectedEmphasis = _protectedEmphasis;
        f._reverseVideo = _reverseVideo;
        f._rightJustified = _rightJustified;
        f._tabStop = _tabStop;
        f._textColor = _textColor;
        f._isEnabled = _isEnabled;
        return f;
    }

    @Override public boolean isExplicit() { return true; }
    @Override public boolean isImplicit() { return false; }

    @Override public UTSColor getBackgroundColor() { return _backgroundColor; }
    @Override public Intensity getIntensity() { return _intensity; }
    @Override public UTSColor getTextColor() { return _textColor; }
    @Override public boolean isAlphabeticOnly() { return _alphabeticOnly && _isEnabled; }
    @Override public boolean isBlinking() { return _blinking; }
    @Override public boolean isChanged() { return _changed; }
    @Override public boolean isNumericOnly() { return _numericOnly && _isEnabled; }
    @Override public boolean isProtected() { return _protected && _isEnabled; }
    @Override public boolean isProtectedEmphasis() { return _protectedEmphasis && _isEnabled; }
    @Override public boolean isReverseVideo() { return _reverseVideo; }
    @Override public boolean isRightJustified() { return _rightJustified && _isEnabled; }
    @Override public boolean isTabStop() { return _tabStop; }

    @Override public Field setAlphabeticOnly(final boolean isAlphabeticOnly) { _alphabeticOnly = isAlphabeticOnly; return this; }
    @Override public Field setBackgroundColor(final UTSColor backgroundColor) { _backgroundColor = backgroundColor; return this; }
    @Override public Field setBlinking(final boolean blinking) { _blinking = blinking; return this; }
    @Override public Field setChanged(final boolean isChanged) { _changed = isChanged; return this; }
    @Override public Field setEnabled(final boolean enabled) { _isEnabled = enabled; return this; }
    @Override public Field setIntensity(final Intensity intensity) { _intensity = intensity; return this; }
    @Override public Field setNumericOnly(final boolean isNumericOnly) { _numericOnly = isNumericOnly; return this; }
    @Override public Field setProtected(final boolean isProtected) { _protected = isProtected; return this; }
    @Override public Field setProtectedEmphasis(final boolean isProtected) { _protectedEmphasis = isProtected; return this; }
    @Override public Field setReverseVideo(final boolean reverseVideo) { _reverseVideo = reverseVideo; return this; }
    @Override public Field setRightJustified(final boolean isRightJustified) { _rightJustified = isRightJustified; return this; }
    @Override public Field setTabStop(final boolean tabStop) { _tabStop = tabStop; return this; }
    @Override public Field setTextColor(final UTSColor textColor) { _textColor = textColor; return this; }
}
