/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;
import java.util.Objects;

public class DUKeyinHandler extends KeyinHandler implements Runnable {

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
    public void abort(){}

    @Override
    public boolean checkSyntax() {
        return _options == null && _arguments != null && _arguments.equalsIgnoreCase("MP");
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
            var filename = Exec.getInstance().dump(true);
            String msg;
            if (filename == null) {
                msg = "Failed to create system dump";
            } else {
                msg = "Created system dump file " + filename;
            }
            Exec.getInstance().sendExecReadOnlyMessage(msg, null);
        } catch (Throwable t) {
            LogManager.logCatching(COMMAND, t);
            Exec.getInstance().stop(StopCode.ExecContingencyHandler);
        }
        setFinished();
    }
}
