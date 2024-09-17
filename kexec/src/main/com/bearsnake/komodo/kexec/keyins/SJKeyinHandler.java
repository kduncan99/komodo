/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;

class SJKeyinHandler extends JumpKeyHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "SJ [ALL | key,...",
        "Sets the indicated jump keys",
    };

    private static final String[] SYNTAX_TEXT = {
        "SJ [ALL | key,...",
    };

    public static final String COMMAND = "SJ";

    public SJKeyinHandler(final ConsoleId source,
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
