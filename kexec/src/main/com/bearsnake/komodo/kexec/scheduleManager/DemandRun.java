/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exec.Exec;

public class DemandRun extends Run implements Runnable {

    protected boolean _isSuspended = false; // TODO can you suspend a demand run?

    public DemandRun(final String actualRunId,
                     final RunCardInfo runCardInfo
    ) {
        super(RunType.Demand, actualRunId, runCardInfo);
        var exec = Exec.getInstance();
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
