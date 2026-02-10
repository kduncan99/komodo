/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.panes;

import com.bearsnake.komodo.kutelib.FieldAttributes;

public class ExplicitField extends Field {

    private final FieldAttributes _attributes;
    private boolean  _isEnabled = true;

    public ExplicitField(final Coordinates coordinates,
                         final FieldAttributes attributes) {
        super(coordinates);
        _attributes = attributes.copy();
    }

    @Override
    public Field copy(final Coordinates coordinates) {
        return new ExplicitField(coordinates.copy(), _attributes.copy());
    }

    public FieldAttributes getAttributes() { return _attributes.copy(); }

    @Override public boolean isExplicit() { return true; }
    @Override public boolean isImplicit() { return false; }

    @Override public UTSColor getBackgroundColor() { return _attributes.getBackgroundColor(); }
    @Override public Intensity getIntensity() { return _attributes.getIntensity(); }
    @Override public UTSColor getTextColor() { return _attributes.getTextColor(); }
    @Override public boolean isAlphabeticOnly() { return _attributes.isAlphabeticOnly() && _isEnabled; }
    @Override public boolean isBlinking() { return _attributes.isBlinking(); }
    @Override public boolean isChanged() { return _attributes.isChanged(); }
    @Override public boolean isNumericOnly() { return _attributes.isNumericOnly() && _isEnabled; }
    @Override public boolean isProtected() { return _attributes.isProtected() && _isEnabled; }
    @Override public boolean isProtectedEmphasis() { return _attributes.isProtectedEmphasis() && _isEnabled; }
    @Override public boolean isReverseVideo() { return _attributes.isReverseVideo(); }
    @Override public boolean isRightJustified() { return _attributes.isRightJustified() && _isEnabled; }
    @Override public boolean isTabStop() { return _attributes.isTabStop(); }

    @Override public Field setAlphabeticOnly(final boolean isAlphabeticOnly) { _attributes.setAlphabeticOnly(isAlphabeticOnly); return this;}
    @Override public Field setBackgroundColor(final UTSColor backgroundColor) { _attributes.setBackgroundColor(backgroundColor); return this;}
    @Override public Field setBlinking(final boolean blinking) { _attributes.setBlinking(blinking); return this;}
    @Override public Field setChanged(final boolean isChanged) { _attributes.setChanged(isChanged); return this; }
    @Override public Field setCoordinates(Coordinates cursorPosition) { _coordinates = cursorPosition; return this; }
    @Override public Field setEnabled(final boolean enabled) { _isEnabled = enabled; return this; }
    @Override public Field setIntensity(final Intensity intensity) { _attributes.setIntensity(intensity); return this; }
    @Override public Field setNumericOnly(final boolean isNumericOnly) { _attributes.setNumericOnly(isNumericOnly); return this;}
    @Override public Field setProtected(final boolean isProtected) { _attributes.setProtected(isProtected); return this; }
    @Override public Field setProtectedEmphasis(final boolean isProtected) { _attributes.setProtectedEmphasis(isProtected); return this; }
    @Override public Field setReverseVideo(final boolean reverseVideo) { _attributes.setReverseVideo(reverseVideo); return this; }
    @Override public Field setRightJustified(final boolean isRightJustified) { _attributes.setRightJustified(isRightJustified); return this;}
    @Override public Field setTabStop(final boolean tabStop) { _attributes.setTabStop(tabStop); return this; }
    @Override public Field setTextColor(final UTSColor textColor) { _attributes.setTextColor(textColor); return this; }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(String.format("Expl[%03d:%03d]", getCoordinates().getRow(), getCoordinates().getColumn()));
        sb.append(":").append(_attributes.toString());
        return sb.toString();
    }
}
