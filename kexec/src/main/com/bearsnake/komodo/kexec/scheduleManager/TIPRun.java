/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;

public class TIPRun extends Run {

    public TIPRun(final String actualRunId,
                  final RunCardInfo runCardInfo
    ) {
        super(RunType.TIP, actualRunId, runCardInfo);
    }

    @Override public final boolean isFinished() { return false; } // TODO
    @Override public final boolean isStarted() { return true; } // TODO
    @Override public final boolean isSuspended() { return false; }
}
