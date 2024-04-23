/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

public class ExecRunControlEntry extends RunControlEntry {

    public ExecRunControlEntry(final String masterAccount) {
        super(RunType.Exec, "EXEC-8", "EXEC-8", "SYS$", masterAccount, "EXEC8");
    }

    @Override public final boolean isFinished() { return false; }
    @Override public final boolean isStarted() { return true; }
    @Override public final boolean isSuspended() { return false; }
}
