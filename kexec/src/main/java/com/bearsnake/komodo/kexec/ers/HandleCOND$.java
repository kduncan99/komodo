/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityStatePacket;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.kexec.scheduleManager.Run;

public class HandleCOND$ extends ERHandler {

    @Override
    public void handle(
        final Run run,
        final ActivityStatePacket activityState
    ) {
        activityState.getGeneralRegisterSet().setRegister(Constants.GRS_A0, run.getRunConditionWord().getWord36());
    }
}
