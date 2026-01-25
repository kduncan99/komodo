/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.keypads;

import com.bearsnake.komodo.kutelib.Terminal;

/**
 * Interface for various keypads.
 * The keypad affects whichever terminal is currently active.
 * For some applications, there may be only one.
 * For these applications, the currently active terminal needs to be sset only once.
 * For applications using a multiple Terminal objects, the application should update the currently active terminal as appropriate.
 * Applications using a TerminalStack can register these KeyPad objects with the TerminalStack for automatic handling.
 */
public interface KeyPad {

    void setActiveTerminal(final Terminal terminal);
}
