/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

class BLKeyinHandler extends KeyinHandler {

    private static enum Action {
        DisplaySummary,
        MoveExistingQueue,
        MoveFuture,
    }

    private static final String[] HELP_TEXT = {
        "BL[,NU] [Dnnn] [runid,...]\n",
        "  Lists runs currently in backlog.",
        // TODO more description
    };

    public static final String COMMAND = "BL";

    public BLKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        return true; // TODO
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        var genf = Exec.getInstance().getGenFileInterface();
        return genf != null && genf.isReady();
    }

    @Override
    void process() {
    }
}
