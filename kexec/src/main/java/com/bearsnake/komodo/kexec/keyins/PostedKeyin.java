/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;

public class PostedKeyin {

    private final ConsoleId _consoleIdentifier;
    private final String _text;

    public PostedKeyin(final ConsoleId consoleId,
                       final String text) {
        _consoleIdentifier = consoleId;
        _text = text.trim();
    }

    public final ConsoleId getConsoleIdentifier() { return _consoleIdentifier; }
    public final String getText() { return _text; }
}
