/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

class DUKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "DU MP",
        "Creates a system dump file",
    };

    public static final String COMMAND = "DU";

    public DUKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        return _options == null && _arguments != null && _arguments.equalsIgnoreCase("MP");
    }

    @Override
    String getCommand() { return COMMAND; }

    @Override
    String[] getHelp() { return HELP_TEXT; }

    @Override
    boolean isAllowed() {
        return true;
    }

    @Override
    void process() {
        var filename = Exec.getInstance().dump(true);
        String msg;
        if (filename == null) {
            msg = "Failed to create system dump";
        } else {
            msg = "Created system dump file " + filename;
        }
        Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
    }
}
