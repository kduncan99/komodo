/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exec.Exec;

/**
 * Handles a DEMAND run.
 * This is not the DEMAND session, which is managed by RSI, but the subset thereof which is mostly in common with BatchRun.
 */
public class DemandRun extends ControlStatementRun implements Runnable {

    public DemandRun(final String actualRunId,
                     final RunCardInfo runCardInfo
    ) {
        super(RunType.Demand, actualRunId, runCardInfo);
        var exec = Exec.getInstance();
    }

    /**
     * To be invoked when the run is allowed to start (usually immediately).
     */
    @Override
    public void run() {
        // TODO handle other sign-on things? Or is that already done by RSI?
        postStartMessage();
        // TODO handle run termination task, if any
        // TODO release and delete READ$
        // TODO release all assigned facilities
        postFinMessage();
        // TODO post job and summary accounting report (maybe) - see ECL doc appdx. E
        // TODO release and disposition PRINT$
        // TODO release and disposition PUNCH$ (if any)
        _isFinished = true;
    }

    public void startRun() {
        _isStarted = true;
        _thread = new Thread(this);
        _thread.start();
    }
}
