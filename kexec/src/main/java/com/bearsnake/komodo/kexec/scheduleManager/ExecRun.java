/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.scheduleManager;

import com.bearsnake.komodo.kexec.configuration.parameters.Tag;
import com.bearsnake.komodo.kexec.csi.RunCardInfo;
import com.bearsnake.komodo.kexec.exec.Exec;

public abstract class ExecRun extends Run {

    public static final String EXEC_RUN_ID = "EXEC-8";
    public static final String EXEC_ACCOUNT_ID = "SYSTEM";
    public static final String EXEC_PROJECT_ID = "SYS$";
    public static final String EXEC_USER_ID = "EXEC8";

    public static final RunCardInfo EXEC_RUN_CARD_INFO =
        new RunCardInfo("").setRunId(EXEC_RUN_ID)
                           .setAccountId(EXEC_ACCOUNT_ID)
                           .setProjectId(EXEC_PROJECT_ID)
                           .setUserId(EXEC_USER_ID);

    protected ExecRun() {
        super(RunType.Exec, EXEC_RUN_ID, EXEC_RUN_CARD_INFO);

        _currentCardLimit = 0;
        _currentPageLimit = 0;
        _currentTimeLimit = 0;
        _defaultQualifier = _runCardInfo.getProjectId();
        _impliedQualifier = _runCardInfo.getProjectId();
    }

    @Override public String getAccountId() { return Exec.getInstance().getConfiguration().getStringValue(Tag.MSTRACC); }
    @Override public final boolean isFinished() { return false; }
    @Override public final boolean isPrivileged() { return true; }
    @Override public final boolean isStarted() { return true; }
    @Override public final boolean isSuspended() { return false; }

    @Override
    public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode
    ) {
        // TODO - exec stop
    }

    @Override
    public void postContingency(
        final int contingencyType,
        final int errorType,
        final int errorCode,
        final long auxiliary
    ) {
        // TODO - exec stop
    }
}
