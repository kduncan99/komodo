/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

class BKeyinHandler extends KeyinHandler {

    private static final String[] HELP_TEXT = {
        "B [ max_runs ]",
        "  Displays or sets the maximum number of batch runs allowed, 0 to 99999.",
    };

    public static final String COMMAND = "B";

    private Integer _maxRuns;

    public BKeyinHandler(final ConsoleId source,
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
        if (_maxRuns != null) {
            // TODO
        }
        // TODO display
    }
}
