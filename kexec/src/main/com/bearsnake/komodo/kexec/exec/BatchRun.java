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
        //IMPROPER RUNSTREAM IN FILE
        //The first image in the file or element specified on the @START statement is an invalid @RUN statement.
        //MAX CARDS
        //The estimated card output has been exceeded. A system generation parameter or the C option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX PAGES
        //The estimated page output has been exceeded. A system generation parameter or the P option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX TIME
        //The estimated running time has been exceeded. A system generation parameter or the T option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX VOLUNTARY DELAY TIME
        //This message appears on the tail sheet of any batch run that exceeds its quota of voluntary delay time. This message appears only if the run has a quota abort.
        //MISSING ACCOUNT
        //The account is required, but it is not specified on the @RUN statement. If you submitted the run from a demand terminal, you can enter another @RUN statement. Batch runs are terminated.
        //MISSING USER-ID
        //The user-id is required, but it is not specified on the @RUN statement. If the run originated from a demand device, another @RUN statement can be entered. Batch runs are terminated.
        //OPERATOR REMOVED RUN
        //The operator removed the run from the backlog by using the RM keyin. A possible solution to eliminate receiving this error is to have valid user-id, account number, and characters in your run-id.
        //OPERATOR TERMINATED RUN BY AN E-KEYIN
        //The operator replied with an E to terminate the run, or a demand user entered an @@X T.
        //OPERATOR TERMINATED RUN BY AN X-KEYIN
        //The operator entered an X keyin to terminate the run.
        //OPERATOR TERMINATED RUN KILLED VIA AN E-KEYIN
        //The operator entered an E keyin for this run. When used for a demand run, an E keyin terminates the task that is currently executing.
        //OPERATOR TERMINATED RUN KILLED VIA AN X-KEYIN
        //The operator entered an X keyin for this run. When used for a demand run, an X keyin terminates the task that is currently executing.
        //READ$FILE REJECT STATUS xxxxxx
        //The READ$ file cannot be assigned.
        //*RUN ALREADY ACTIVE*
        //An @RUN statement was received while in demand run mode.
        //  *RUN-ID NOT ACTIVE*
        //The run-id on an @@TM is not currently active.
        //  RUN REMOVED DUE TO INVALID USER-ID/PASSWORD
        //Either the user-id or the password on the @PASSWD statement is illegal.
        //  RUN REMOVED DUE TO SECURITY ERROR
        //The user’s run has been removed due to an illegal password, or a password was not found in the runstream.
        //  RUNSTREAM ANALYSIS TERMINATED
        //The run has been terminated because of an error condition, and the remaining control statements are not processed.
    }
}
