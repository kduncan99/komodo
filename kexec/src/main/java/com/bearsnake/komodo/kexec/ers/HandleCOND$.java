/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityState;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.kexec.scheduleManager.Run;

public class HandleCOND$ extends ERHandler {

    @Override
    public void handle(
        final Run run,
        final ActivityState activityState
    ) {
        var a0 = activityState.getGeneralRegisterSet().getRegister(GeneralRegisterSet.A0);
        a0.setW(run.getRunConditionWord().getW());
    }
}
