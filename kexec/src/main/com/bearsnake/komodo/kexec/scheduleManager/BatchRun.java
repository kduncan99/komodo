/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;

public class BatchRun extends ControlStatementRun implements Runnable {

    // Runstream is still being read, have not yet reached @FIN TODO -- is this possible? Do we actually have a BatchRun before we finish reading input?
    protected boolean _waitingOnFin = true;

    public BatchRun(final String actualRunId,
                    final RunCardInfo runCardInfo) {
        super(RunType.Batch, actualRunId, runCardInfo);
        _inputQueueAddress = 0;
    }

    /**
     * To be invoked when the run comes out of backlog.
     * The first image is a @RUN card, but it has already been processed by the exec (else we would not exist).
     * So, we just start reading images (ignoring the initial @RUN card other than printing it to PRINT$) and processing them.
     */
    @Override
    public void run() {
        try {
            postStartMessageToConsole();
            if (!setup()) {
                postToPrint("RUNSTREAM ANALYSIS TERMINATED", 1);
            } else {
                // TODO loop on reading images
            }

            // TODO handle run termination task, if any
            // TODO release and disposition PUNCH$ (if any)
            // TODO release all assigned facilities EXCEPT for PRINT$
            postFinMessageToConsole();
            // TODO post job and summary accounting report (maybe) - see ECL doc appdx. E
            // TODO release and disposition PRINT$
            _isFinished = true;
            // TODO scheduler.unRegisterRun()
        } catch (ExecStoppedException ex) {
            // Nothing to be done here
        }

        //IMPROPER RUNSTREAM IN FILE
        // The first image in the file or element specified on the @START statement is an invalid @RUN statement.
        //MAX CARDS
        // The estimated card output has been exceeded.
        // A system generation parameter or the C option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX PAGES
        // The estimated page output has been exceeded.
        // A system generation parameter or the P option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX TIME
        // The estimated running time has been exceeded.
        // A system generation parameter or the T option on the @RUN statement specified that the run be aborted if this occurred.
        //MAX VOLUNTARY DELAY TIME
        // This message appears on the tail sheet of any batch run that exceeds its quota of voluntary delay time.
        // This message appears only if the run has a quota abort.
        //MISSING ACCOUNT
        // The account is required, but it is not specified on the @RUN statement.
        //  If you submitted the run from a demand terminal, you can enter another @RUN statement.
        //  Batch runs are terminated.
        //MISSING USER-ID
        // The user-id is required, but it is not specified on the @RUN statement.
        // If the run originated from a demand device, another @RUN statement can be entered.
        // Batch runs are terminated.
        //OPERATOR REMOVED RUN
        // The operator removed the run from the backlog by using the RM keyin.
        // A possible solution to eliminate receiving this error is to have valid user-id, account number, and characters in your run-id.
        //OPERATOR TERMINATED RUN BY AN E-KEYIN
        // The operator replied with an E to terminate the run, or a demand user entered an @@X T.
        //OPERATOR TERMINATED RUN BY AN X-KEYIN
        // The operator entered an X keyin to terminate the run.
        //OPERATOR TERMINATED RUN KILLED VIA AN E-KEYIN
        // The operator entered an E keyin for this run. When used for a demand run, an E keyin terminates the task that is currently executing.
        //OPERATOR TERMINATED RUN KILLED VIA AN X-KEYIN
        // The operator entered an X keyin for this run. When used for a demand run, an X keyin terminates the task that is currently executing.
        //PUNCH FILE CANNOT BE PUNCHED: NO PUNCH DEVICES CONFIGURED
        //  Punch device is not configured.
        // READ$FILE REJECT STATUS xxxxxx
        // The READ$ file cannot be assigned.
        //*RUN ALREADY ACTIVE*
        // An @RUN statement was received while in demand run mode.
        //*RUN-ID NOT ACTIVE*
        // The run-id on an @@TM is not currently active.
        //RUN REMOVED DUE TO INVALID USER-ID/PASSWORD
        // Either the user-id or the password on the @PASSWD statement is illegal.
        //RUN REMOVED DUE TO SECURITY ERROR
        // The user’s run has been removed due to an illegal password, or a password was not found in the runstream.
        //RUNSTREAM ANALYSIS TERMINATED
        // The run has been terminated because of an error condition, and the remaining control statements are not processed.
    }

    public void startRun() {
        _isStarted = true;
        _thread = new Thread(this);
        _thread.start();
    }

    private boolean setup() throws ExecStoppedException {
        var fsResult = assignREAD$File();
        if ((fsResult.getStatusWord() & 0_400000_000000L) != 0) {
            return false;
        }

        // TODO assign PRINT$ file
        // TODO check RUN card artifacts

        return true;
    }
}
