/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityState;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;

public class HandleSETC$ extends ERHandler {

    @Override
    public void handle(
        final RunControlEntry runControlEntry,
        final ActivityState activityState
    ) {
        var a0 = activityState.getGeneralRegisterSet().getRegister(GeneralRegisterSet.A0);
        if (a0.isPositive()) {
            runControlEntry.getRunConditionWord().setT3(a0.getW());
        } else {
            long mask = 0_0030_0000_7777L;
            long notMask = 0_7747_7777_0000L;
            var rcw = runControlEntry.getRunConditionWord();
            synchronized (rcw) {
                var value = rcw.getW() & notMask;
                value |= a0.getW() & mask;
                rcw.setW(value);
            }
        }
    }
}
