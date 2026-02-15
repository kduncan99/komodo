/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.panes;

import com.bearsnake.komodo.utslib.Coordinates;

public class ScreenRegion {

    private final Coordinates _startingPosition;
    private final Coordinates _endingPosition;
    private final int         _count;

    public ScreenRegion(final Coordinates startingPosition,
                        final Coordinates endingPosition,
                        final int count) {
        _startingPosition = startingPosition;
        _endingPosition = endingPosition;
        _count = count;
    }

    public Coordinates getStartingPosition() { return _startingPosition; }
    public Coordinates getEndingPosition() { return _endingPosition; }
    public int getCount() { return _count; }
}
