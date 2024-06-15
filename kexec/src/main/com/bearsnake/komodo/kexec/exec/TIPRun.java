/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

public class TIPRun extends Run {

    public TIPRun(String runId,
                  String originalRunId,
                  String projectId,
                  String accountId,
                  String userId) {
        super(RunType.TIP, runId, originalRunId, projectId, accountId, userId);
        _cardLimit = Exec.getInstance().getConfiguration().getMaxCards();
        _pageLimit = Exec.getInstance().getConfiguration().getMaxPages();
    }

    @Override public final boolean isFinished() { return false; } // TODO
    @Override public final boolean isStarted() { return true; } // TODO
    @Override public final boolean isSuspended() { return false; }
}
