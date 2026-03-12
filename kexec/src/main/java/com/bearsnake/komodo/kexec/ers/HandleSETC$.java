/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityStatePacket;
import com.bearsnake.komodo.engine.Constants;
import com.bearsnake.komodo.engine.GeneralRegisterSet;
import com.bearsnake.komodo.kexec.scheduleManager.Run;

public class HandleSETC$ extends ERHandler {

    @Override
    public void handle(
        final Run run,
        final ActivityStatePacket activityState,
        final GeneralRegisterSet generalRegisterSet
    ) {
        var a0Reg = generalRegisterSet.getRegister(Constants.GRS_A0);
        if (a0Reg.isPositive()) {
            run.getRunConditionWord().setERSetCValue(a0Reg.getValue());
        } else {
            long mask = 0_0030_0000_7777L;
            var rcw = run.getRunConditionWord();
            synchronized (rcw) {
                var value = rcw.getWord36();
                value &= ~mask;
                value |= a0Reg.getValue() & mask;
                rcw.setWord36(value);
            }
        }
    }
}
