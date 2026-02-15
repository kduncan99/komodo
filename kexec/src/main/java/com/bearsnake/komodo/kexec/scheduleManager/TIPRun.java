/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.csi.RunCardInfo;

public class TIPRun extends NonExecRun {

    public TIPRun(final String actualRunId,
                  final RunCardInfo runCardInfo
    ) {
        super(RunType.TIP, actualRunId, runCardInfo);
    }
}
