/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

public class BatchRun extends Run implements Runnable {

    protected boolean _isSuspended = false;

    public BatchRun(String runId,
                    String originalRunId,
                    String projectId,
                    String accountId,
                    String userId) {
        super(RunType.Batch, runId, originalRunId, projectId, accountId, userId);
        var exec = Exec.getInstance();
        _cardLimit = exec.getConfiguration().getMaxCards();
        _pageLimit = exec.getConfiguration().getMaxPages();
    }

    @Override public final boolean isFinished() { return false; } // TODO
    @Override public final boolean isStarted() { return true; } // TODO
    @Override public final boolean isSuspended() { return _isSuspended; }

    /**
     * To be invoked when the run comes out of backlog.
     * The first image is a @RUN card, but it has already been processed by the exec (else we would not exist).
     * So, we just start reading images (ignoring the initial @RUN card other than printing it to PRINT$)
     */
    public void run() {
        // TODO
    }
}
