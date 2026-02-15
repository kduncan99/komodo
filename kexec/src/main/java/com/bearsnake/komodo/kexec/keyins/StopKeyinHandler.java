/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;

class StopKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "$!",
        "Initiates auto-recovery of the operating system",
    };

    private static final String[] SYNTAX_TEXT = {
        "$!",
    };

    public static final String COMMAND = "$!";

    public StopKeyinHandler(final ConsoleId source,
                            final String options,
                            final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        return true;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        return true;
    }

    @Override
    void process() {
        Exec.getInstance().stop(StopCode.OperatorInitiatedRecovery);
    }
}
