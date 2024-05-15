/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityState;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;

public class HandleCOND$ extends ERHandler {

    @Override
    public void handle(
        final RunControlEntry runControlEntry,
        final ActivityState activityState
    ) {
        var a0 = activityState.getGeneralRegisterSet().getRegister(GeneralRegisterSet.A0);
        a0.setW(runControlEntry.getRunConditionWord().getW());
    }
}
