/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

public class CJKeyinHandler extends JumpKeyHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "CJ[,ALL]", // TODO "ALL" should be an argument, not an option
        "CJ {key}[,...]",
        "Clears the indicated jump keys",
    };

    public static final String COMMAND = "CJ";

    public CJKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    public String getCommand() { return COMMAND; }

    @Override
    public String[] getHelp() { return HELP_TEXT; }

    @Override
    public void run() {
        try {
            process(false);
        } catch (Throwable t) {
            LogManager.logCatching(COMMAND, t);
            Exec.getInstance().stop(StopCode.ExecContingencyHandler);
        }
        setFinished();
    }
}
