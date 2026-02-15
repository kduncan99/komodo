/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;

public class CJKeyinHandler extends JumpKeyHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "CJ [ALL | key,...",
        "Clears the indicated jump keys",
    };

    private static final String[] SYNTAX_TEXT = {
        "CJ [ALL | key,...",
    };

    public static final String COMMAND = "CJ";

    public CJKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    final boolean checkSyntax() {
        if (_options != null) {
            return false;
        }

        return checkJumpKeyArguments();
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    void process() {
        setOrClearJumpKeys(false);
    }
}
