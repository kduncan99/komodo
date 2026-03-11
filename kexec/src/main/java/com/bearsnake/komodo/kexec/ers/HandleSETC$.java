/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityStatePacket;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.kexec.scheduleManager.Run;
import com.bearsnake.komodo.baselib.Word36;

public class HandleSETC$ extends ERHandler {

    @Override
    public void handle(
        final Run run,
        final ActivityStatePacket activityState
    ) {
        var a0 = activityState.getGeneralRegisterSet().getRegisterValue(GeneralRegisterSet.GRS_A0);
        if (Word36.isPositive(a0)) {
            run.getRunConditionWord().setERSetCValue(a0);
        } else {
            long mask = 0_0030_0000_7777L;
            var rcw = run.getRunConditionWord();
            synchronized (rcw) {
                var value = rcw.getWord36();
                value &= ~mask;
                value |= a0 & mask;
                rcw.setWord36(value);
            }
        }
    }
}
