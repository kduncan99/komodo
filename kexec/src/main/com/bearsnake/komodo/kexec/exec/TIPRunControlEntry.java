/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

public class TIPRunControlEntry extends RunControlEntry {

    public TIPRunControlEntry(RunType runType,
                              String runId,
                              String originalRunId,
                              String projectId,
                              String accountId,
                              String userId) {
        super(runType, runId, originalRunId, projectId, accountId, userId);
    }

    @Override public final boolean isFinished() { return false; } // TODO
    @Override public final boolean isStarted() { return true; } // TODO
}
