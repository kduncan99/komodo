/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

public class LGKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "LG {message}",
        "Creates an entry in the system log",
    };

    public static final String COMMAND = "LG";

    public LGKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    public void abort(){}

    @Override
    public boolean checkSyntax() {
        return _options == null && _arguments != null;
    }

    @Override
    public String getCommand() { return COMMAND; }

    @Override
    public String[] getHelp() { return HELP_TEXT; }

    @Override
    public boolean isAllowed() {
        return true;
    }

    @Override
    public void run() {
        try {
            LogManager.logInfo(COMMAND, "%s", _arguments);
            var msg = "Log entry created";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
        } catch (Throwable t) {
            LogManager.logCatching(COMMAND, t);
            Exec.getInstance().stop(StopCode.ExecContingencyHandler);
        }
        setFinished();
    }
}
