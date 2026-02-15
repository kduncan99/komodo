/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.kexec.configuration.Configuration;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;

public class TestExec extends Exec {

    private static final boolean[] JUMP_KEY_TABLE = new boolean[36];
    private boolean _stopped = false;

    public TestExec() {
        super(JUMP_KEY_TABLE);
        setConfiguration(new Configuration());
    }

    @Override
    public void sendExecReadOnlyMessage(final String message, final ConsoleType consoleType) {
        System.out.printf("[%s][%s]\n", consoleType, message);
    }

    @Override
    public void sendExecReadOnlyMessage(final String message, final ConsoleId consoleId) {
        System.out.printf("[%s][%s]\n", consoleId, message);
    }

    @Override
    public void stop(final StopCode code) {
        _stopped = true;
    }
}
