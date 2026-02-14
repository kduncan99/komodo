/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.utslib.fields;

import com.bearsnake.komodo.utslib.Coordinates;

public abstract class Field {

    protected Coordinates _coordinates;

    protected Field(final Coordinates coordinates) {
        _coordinates = coordinates;
    }

    public final Coordinates getCoordinates() {
        return _coordinates;
    }

    public abstract Field copy(final Coordinates coordinates);

    public abstract boolean isExplicit();
    public abstract boolean isImplicit();

    public abstract UTSColor getBackgroundColor();
    public abstract Intensity getIntensity();
    public abstract UTSColor getTextColor();
    public abstract boolean isAlphabeticOnly();
    public abstract boolean isBlinking();
    public abstract boolean isChanged();
    public abstract boolean isNumericOnly();
    public abstract boolean isProtected();
    public abstract boolean isProtectedEmphasis();
    public abstract boolean isReverseVideo();
    public abstract boolean isRightJustified();
    public abstract boolean isTabStop();

    public abstract Field setAlphabeticOnly(boolean isAlphabeticOnly);
    public abstract Field setBackgroundColor(UTSColor backgroundColor);
    public abstract Field setBlinking(boolean blinking);
    public abstract Field setChanged(boolean isChanged);
    public abstract Field setCoordinates(Coordinates cursorPosition);
    public abstract Field setEnabled(final boolean flag);
    public abstract Field setIntensity(Intensity intensity);
    public abstract Field setNumericOnly(boolean isNumericOnly);
    public abstract Field setProtected(boolean isProtected);
    public abstract Field setProtectedEmphasis(boolean isProtected);
    public abstract Field setReverseVideo(boolean reverseVideo);
    public abstract Field setRightJustified(boolean isRightJustified);
    public abstract Field setTabStop(boolean tabStop);
    public abstract Field setTextColor(UTSColor textColor);

    public abstract String toString();
}
