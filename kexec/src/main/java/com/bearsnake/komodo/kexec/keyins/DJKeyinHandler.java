/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

public class DJKeyinHandler extends JumpKeyHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "DJ",
        "Displays currently-set system jump keys",
    };

    private static final String[] SYNTAX_TEXT = {
        "DJ",
    };

    public static final String COMMAND = "DJ";

    public DJKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        return _options == null && _arguments == null;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    void process() {
        Exec.getInstance().displayJumpKeys(_source);
    }
}
