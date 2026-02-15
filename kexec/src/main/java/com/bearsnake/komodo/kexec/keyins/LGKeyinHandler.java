/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class LGKeyinHandler extends KeyinHandler implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(LGKeyinHandler.class);

    private static final String[] HELP_TEXT = {
        "LG message",
        "Creates an entry in the system log",
    };

    private static final String[] SYNTAX_TEXT = {
        "LG message",
    };

    public static final String COMMAND = "LG";

    public LGKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        return _options == null && _arguments != null;
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
        LOGGER.info("LG: {}", _arguments);
        var msg = "Log entry created";
        Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
    }
}
