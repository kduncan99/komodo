/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

public class DemandRun extends Run implements Runnable {

    protected boolean _isSuspended = false; // TODO can you suspend a demand run?

    public DemandRun(String runId,
                     String originalRunId,
                     String projectId,
                     String accountId,
                     String userId) {
        super(RunType.Demand, runId, originalRunId, projectId, accountId, userId);
        var exec = Exec.getInstance();
        _cardLimit = exec.getConfiguration().getMaxCards();
        _pageLimit = exec.getConfiguration().getMaxPages();
    }

    @Override public final boolean isFinished() { return false; } // TODO
    @Override public final boolean isStarted() { return true; } // TODO
    @Override public final boolean isSuspended() { return _isSuspended; }

    /**
     * To be invoked when RSI is ready for us to do our thing, post sign-on.
     */
    public void run() {
        // TODO
    }
}
