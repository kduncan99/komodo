/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

/**
 * Listener interface for cursor position changes.
 */
public interface CursorPositionListener {

    void notifyCursorPositionChange(final int newRow, final int newColumn);
}
