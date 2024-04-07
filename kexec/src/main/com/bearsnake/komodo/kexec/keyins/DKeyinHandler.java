/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;

public class DKeyinHandler extends KeyinHandler implements Runnable {

    private static final String[] HELP_TEXT = {
        "D",
        "Displays system date and time",
    };

    public static final String COMMAND = "D";

    public DKeyinHandler(final ConsoleId source,
                         final String options,
                         final String arguments) {
        super(source, options, arguments);
    }

    @Override
    public void abort(){}

    @Override
    public boolean checkSyntax() {
        return _options.isBlank() && _arguments.isBlank();
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
        Exec.getInstance().displayDateAndTime();
        setFinished();
    }
}
