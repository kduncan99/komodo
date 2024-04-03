/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import java.util.concurrent.TimeUnit;

public class StopKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "$!",
        "Initiates auto-recovery of the operating system",
    };

    public static final String COMMAND = "$!";

    public StopKeyinHandler(final ConsoleId source,
                            final String options,
                            final String arguments) {
        super(source, options, arguments);
    }

    public void abort(){}

    public boolean checkSyntax() {
        return _options.isBlank() && _arguments.isBlank();
    }

    public String getCommand() { return COMMAND; }
    public String[] getHelp() { return HELP_TEXT; }

    public void invoke() {
        Exec.getInstance().getExecutor().schedule(this, 0, TimeUnit.MILLISECONDS);
    }

    public boolean isAllowed() {
        return true;
    }

    @Override
    public void run() {
        Exec.getInstance().stop(StopCode.OperatorInitiatedRecovery);
        setFinished();
    }
}
