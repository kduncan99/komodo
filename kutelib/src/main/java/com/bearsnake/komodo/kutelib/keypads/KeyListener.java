/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import javafx.scene.layout.Pane;

/**
 * Listener interface for KuteButton
 */
public interface KeyListener {
    /**
     * Invoked when a KuteButton is clicked while enabled
     * @param source the pane that contains the button
     * @param id the identifier configured for the button
     */
    void notify(final Pane source, final int id);

    /**
     * Invoked when a KuteButton is released
     * @param source the pane that contains the button
     * @param id the identifier configured for the button
     */
    void notifyReleased(final Pane source, final int id);
}
