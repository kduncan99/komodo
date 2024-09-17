/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

class CSKeyinHandler extends KeyinHandler {

    private static final String[] HELP_TEXT = {
        "CS [A | AD | ALL | AT | H | HD | HT]",
        "CS[,NU] [A | H] ident[,...]",
        "CS runid* [Px | Dhhmm | Shhmm | Ly | F]",
        // TODO help text
    };

    private static final String[] SYNTAX_TEXT = {
        "CS [A | AD | ALL | AT | H | HD | HT]",
        "CS[,NU] [A | H] ident[,...]",
        "CS runid* [Px | Dhhmm | Shhmm | Ly | F]",
    };

    public static final String COMMAND = "CS";

    public CSKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        // TODO
        return true;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        var genf = Exec.getInstance().getGenFileInterface();
        return genf != null && genf.isReady();
    }

    @Override
    void process() {
        // TODO
    }
}
