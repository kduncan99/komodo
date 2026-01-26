/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

public final class ImplicitField extends Field {

    public ImplicitField() {
        super(Coordinates.HOME_POSITION);
    }

    @Override
    public Field copy(final Coordinates coordinates) {
        return new ImplicitField();
    }

    @Override public boolean isExplicit() { return false; }
    @Override public boolean isImplicit() { return true; }

    @Override public UTSColor getBackgroundColor() { return null; }
    @Override public Intensity getIntensity() { return Intensity.NORMAL; }
    @Override public UTSColor getTextColor() { return null; }
    @Override public boolean isAlphabeticOnly() { return false; }
    @Override public boolean isBlinking() { return false; }
    @Override public boolean isChanged() { return false; }
    @Override public boolean isNumericOnly() { return false; }
    @Override public boolean isProtected() { return false; }
    @Override public boolean isProtectedEmphasis() { return false; }
    @Override public boolean isReverseVideo() { return false; }
    @Override public boolean isRightJustified() { return false; }
    @Override public boolean isTabStop() { return false; }

    @Override public Field setAlphabeticOnly(boolean isAlphabeticOnly) { return this; }
    @Override public Field setBackgroundColor(UTSColor backgroundColor) { return this; }
    @Override public Field setBlinking(boolean blinking) { return this; }
    @Override public Field setChanged(boolean isChanged) { return this; }
    @Override public Field setCoordinates(Coordinates cursorPosition) { return this; }
    @Override public Field setEnabled(final boolean flag) { return this; }
    @Override public Field setIntensity(Intensity intensity) { return this; }
    @Override public Field setNumericOnly(boolean isNumericOnly) { return this; }
    @Override public Field setProtected(boolean isProtected) { return this; }
    @Override public Field setProtectedEmphasis(boolean isProtected) { return this; }
    @Override public Field setReverseVideo(boolean reverseVideo) { return this; }
    @Override public Field setRightJustified(boolean isRightJustified) { return this; }
    @Override public Field setTabStop(boolean tabStop) { return this; }
    @Override public Field setTextColor(UTSColor textColor) { return this; }
}
